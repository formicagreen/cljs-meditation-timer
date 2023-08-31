(ns app.state
  (:require
   [reagent.core :as r]
   [app.core :as core]
   ["@capacitor/core" :refer [Capacitor]]
   ["@capacitor/preferences" :refer [Preferences]]
   ["@capacitor-community/keep-awake" :refer [KeepAwake]]
   ["@capacitor/local-notifications" :refer [LocalNotifications]]))


(comment


  (->
   (.isSupported KeepAwake)
   (.then (fn [supported] (println "KeepAwake supported? " supported)))
   (.catch (fn [err] (println err))))
  (->
   (.isKeptAwake KeepAwake)
   (.then (fn [is-kept-awake] (println "Kept awake? " is-kept-awake)))
   (.catch (fn [err] (println err))))


  #rtrace (reduce + (map inc (range 10)))


  (.schedule LocalNotifications
             (clj->js {:notifications
                       [{:title "hey"
                         :body "what"
                         :sound "/public/singing-bowl.wav"
                         :id 5}]}))

  (.requestPermissions LocalNotifications)

  (->
   (.checkPermissions LocalNotifications)
   (.then (fn [res] (println res)))
   (.catch (fn [err] (println err))))

  (js/alert "foo"))


; App state that is 1. not persistent 2. doesn't update super frequently
(defonce session
  (r/atom core/initial-state))


; Persistent state, stored using the Capacitor Preferences plugin
(def persistent
  (r/atom core/initial-storage))


; Elapsed time in milliseconds.
(defonce elapsed
  (r/atom 0))


; Scroll positions for scrollable elements.
(defonce scroll-areas
  (r/atom {:timers {:scroll-top 0
                    :client-height 0
                    :scroll-height 0}
           :steps {:scroll-top 0
                   :client-height 0
                   :scroll-height 0}}))


(comment
  @session
  @persistent
  @elapsed)


(defn current-timer []
  (core/where (:timers @persistent) :id (:current-timer @session)))

(comment
  (current-timer))


(defn persist!
  "Update the persistent state and save it to storage."
  [f & args]
  (let [new-data (apply swap! persistent f args)]
    (->
     (.set Preferences #js {:key "storage" :value (pr-str new-data)})
     (.then (fn [prefs] (println prefs)))
     (.catch (fn [err] (println err))))
    new-data))

(comment
  (persist! update :timers conj (core/timer "foo" (core/step 5))))


(defn swap-page! [k]
  (swap! session assoc :page k))


(defn reset-state! []
  (reset! session core/initial-state)
  (reset! elapsed 0))

(defn reset-storage! []
  (persist! assoc :timers (:timers core/initial-storage)))

(comment
  (reset-storage!))


(defn play-sound! [file-path]
  (println "play-sound!")
  (.play (js/Audio. file-path)))


(comment
  (play-sound! "singing-bowl.wav"))


(defn clear-step-timeouts! []
  (doseq [timeout (vals (get-in @session [:timeouts :steps]))]
    (js/clearTimeout timeout))
  (swap! session assoc-in [:timeouts :steps] {}))


(defn clear-end-timeout! []
  (js/clearTimeout (get-in @session [:timeouts :total]))
  (swap! session assoc-in [:timeouts :total] nil))


(defn clear-timeouts! []
  (clear-step-timeouts!)
  (clear-end-timeout!))


(defn end! []
  (.allowSleep KeepAwake)
  (swap! session assoc
         :started-at nil
         :previous-time 0)
  (reset! elapsed 0))


(defn stop! []
  (clear-timeouts!)
  (end!))

(comment
  (stop!))


(defn end-step! [step]
  (println "end-step!")
  (swap! session update-in [:timeouts :steps] dissoc (:id step))
  (js/console.log "end-step! " (clj->js @session))

  (if (empty? (get-in @session [:timeouts :steps]))
    (play-sound! "singing-bowl-double.wav") ; play double sound if last step
    (play-sound! "singing-bowl.wav") ; else play regular sound
    ))

(comment
  (end-step! (-> @persistent :timers first :steps last)))


(defn remaining-time [timer step]
  (-> (core/step-end-time timer step)
      core/min->ms
      (- @elapsed)))


(defn current-step [] (core/current-step (current-timer) @elapsed))

(comment
  (current-step))


(defn remaining-current-step []
  (remaining-time (current-timer) (current-step)))

(defn remaining-total []
  (remaining-time (current-timer) (last (:steps (current-timer)))))


(defn set-timeout! [timer step]
  (let [time-ms (remaining-time timer step)]
    (when (pos? time-ms)
      (println "set-timeout!" time-ms)
      [(:id step) (js/setTimeout #(end-step! step) time-ms)])))


(defn set-timeouts! [timer]
  (let [step-timeouts (into {} (for [step (:steps timer)] (set-timeout! timer step)))
        end-timeout (js/setTimeout #(end!) (remaining-total))]
    {:total end-timeout
     :steps step-timeouts}))


(comment
  (set-timeouts! (-> @persistent :timers first))
  (set-timeout! (-> @persistent :timers first) (-> @persistent :timers first :steps first)))



(defn update-elapsed-time! []
  (reset! elapsed (->
                   (.now js/Date)
                   (- (:started-at @session))
                   (+ (:previous-time @session)))))


(defn animate! []
  (when (= :running (core/play-state @session))
    (update-elapsed-time!)
    (js/requestAnimationFrame animate!)))


(defn start! [timer]
  (play-sound! "singing-bowl.wav")
  (.keepAwake KeepAwake)
  (swap! session assoc
         :current-timer (:id timer)
         :started-at (.now js/Date)
         :timeouts (set-timeouts! timer))
  (animate!))


(defn pause! []
  (println "pause!")
  (clear-timeouts!)
  (.allowSleep KeepAwake)
  (swap! session assoc
         :previous-time @elapsed
         :started-at nil))

(defn resume! []
  (.keepAwake KeepAwake)
  (swap! session assoc
         :started-at (.now js/Date)
         :timeouts (set-timeouts! (current-timer)))
  (animate!))


(defn edit-timer! [timer]
  ; reset scroll position for timer edit page
  (swap! scroll-areas assoc :steps {:scroll-top 0
                                    :client-height 0
                                    :scroll-height 0})
  (swap! session assoc
         :page :edit
         :current-timer (:id timer)))


(defn add-and-edit-timer! []
  (let [name  (str "Timer " (-> @persistent :timers count inc))
        timer (core/timer name [(core/step 5)])]
    (persist! update :timers conj timer)
    (edit-timer! timer)))


(defn show-modal! [modal]
  (swap! session assoc :modal modal))


(defn close-modal! []
  (swap! session assoc :modal nil))


(defn set-notification! [timer step]
  (let [time-ms (remaining-time timer step)]
    (when (pos? time-ms)
      (println "set-notification!" step time-ms)
      (.schedule LocalNotifications
                 (clj->js {:notifications
                           [{:title core/app-name
                             :body (str "Step " (inc (core/step-index timer step)) " of " (-> timer :steps count) " finished")
                             :id (hash (:id step)) ; id must be number
                             :sound (if (core/is-last-step? timer step) "/public/singing-bowl-double.wav" "/public/singing-bowl.wav")
                             :schedule {:at (js/Date. (+ (.now js/Date) time-ms))}}]})))))


(comment
  (set-notification! (-> @persistent :timers first) (-> @persistent :timers first :steps first))

  (.schedule LocalNotifications
             (clj->js {:notifications
                       [{:title "fest"
                         :body "hest"
                         :id 1000 ; id must be number
                         :schedule {:at (js/Date. (+ (.now js/Date) 5000))}}]}))

  (do
    (set! (.-LocalNotifications js/window) LocalNotifications)
    (js/eval "LocalNotifications.schedule({
    notifications: [
      { id: 42, title: 'Hey', body: 'What', schedule: { at: new Date(Date.now() + 1000) } }
    ]})")))


(defn set-notifications! [timer]
  (println "set-notifications!" timer)
  (doseq [step (:steps timer)]
    (set-notification! timer step)))

(comment
  (set-notifications! (-> @persistent :timers first)))


(defn clear-notifications! []
  (.cancel LocalNotifications
           (clj->js
            {:notifications
             (for [step (:steps (current-timer))]
               {:id (hash (:id step))})})))


(defn handle-app-state-change! [state]
  (println "handle-app-state-change!" state)
  (when (= :running (core/play-state @session))
    (if (.-isActive state)
      (do (println "app went to foreground")
          (update-elapsed-time!)
          (clear-notifications!)
          (swap! session assoc :timeouts (set-timeouts! (current-timer))))
      (do (println "app went to background")
          (clear-step-timeouts!)
          (set-notifications! (current-timer))))))


(comment
  (handle-app-state-change! (clj->js {:isActive false}))
  (handle-app-state-change! (clj->js {:isActive true}))
  (start! (first (:timers @persistent)))
  (pause!)
  (resume!)
  (stop!)
  (swap! session assoc :debug true)
  (swap! session assoc :debug false)
  (swap-page! :run)
  (add-and-edit-timer!)
  (reset-state!)
  (show-modal! :about)
  (close-modal!)
  (add-and-edit-timer!)
  (hash (-> @persistent :timers first :steps first :id))
  @session
  @elapsed)

(comment

  (.keepAwake KeepAwake) ; use this when developing on device

  (.-isNative Capacitor)

  (.isPluginAvailable Capacitor "Preferences")

  (.getPlatform Capacitor)

  (->
   (.set Preferences #js {:key "foo" :value "bar"})
   (.then (fn [prefs] (println prefs)))
   (.catch (fn [err] (println err))))

  (->
   (.get Preferences #js {:key "foo"})
   (.then (fn [prefs] (println prefs)))
   (.catch (fn [err] (println err)))))