(ns app.state
  (:require
   [reagent.core :as r]
   [app.core :as core]))


(comment

  (.-isNative Capacitor)

  (.isPluginAvailable Capacitor "Preferences")

  (.getPlatform Capacitor)

  (->
   (.set Preferences "foo" "bar")
   (.then (fn [prefs] (println prefs))))

  (->
   (.get Preferences "foo")
   (.then (fn [prefs] (println prefs)))))


(defonce state (r/atom (core/initial-state)))


(comment
  @state
)


(defonce storage
  (r/atom (js/JSON.parse nil)))

(defn current-timer []
  (core/where (:timers @state) :id (:current-timer @state)))


(defn swap-page! [k]
  (swap! state assoc :page k))


(defn reset-state! []
  (reset! state (core/initial-state)))


(defn add-and-edit-timer! []
  (let [name  (str "Timer " (inc (count (:timers @state))))
        timer (core/timer name)]
    (swap! state
           (fn [s] (-> s
                       (update :timers conj timer)
                       (assoc :current-timer (:id timer)
                              :page :edit))))))


(defn play-sound! []
  (.play (js/Audio. "singing-bowl-high.mp3")))


(comment
  (play-sound!))


(defn clear-timeouts! []
  (doseq [timeout (vals (:timeouts @state))]
    (js/clearTimeout timeout))
  (swap! state assoc
         :timeouts {}
         :animation-frame nil))


(defn stop! []
  (clear-timeouts!)
  (swap! state assoc
         :started-at nil
         :previous-time nil
         :elapsed nil))

(comment
  (stop!)
  )


(defn end-step! [step]
  (println "end-step!")
  (play-sound!)
  (swap! state update :timeouts dissoc (:id step))
  ; when no more timeouts, stop the timer
  (when (empty? (:timeouts @state))
    (stop!)))


(defn remaining-time [timer step]
  (-> (core/duration-inclusive timer step)
      core/min->ms
      (/ (or (:speedup @state) 1))
      (- (:previous-time @state))))


(defn set-timeout! [timer step]
  (let [time-ms (remaining-time timer step)]
    (when (pos? time-ms)
      (println "set-timeout!" time-ms)
      [(:id step) (js/setTimeout #(end-step! step) time-ms)])))

(comment
  (when-let [x 0]
    (println "x is not nil")))


(defn set-timeouts! [timer]
  (into {} (for [step (:steps timer)] (set-timeout! timer step))))


(defn update-elapsed-time! []
  (swap! state assoc
         :elapsed (->
                   (.now js/Date)
                   (- (:started-at @state))
                   (+ (:previous-time @state)))))


(defn running? []
  (:started-at @state))


(defn animate! []
  (when (running?)
    (update-elapsed-time!)
    (js/requestAnimationFrame animate!)))


(defn start! [timer]
  (play-sound!)
  (swap! state assoc
         :current-timer (:id timer)
         :started-at (.now js/Date)
         :timeouts (set-timeouts! timer))
  (animate!))


(defn pause! []
  (clear-timeouts!)
  (swap! state assoc
         :previous-time (:elapsed @state)
         :started-at nil))

(defn resume! []
  (swap! state assoc
         :started-at (.now js/Date)
         :timeouts (set-timeouts! (current-timer)))
  (animate!))


(defn edit-timer! [timer]
  (swap! state assoc
         :page :edit
         :current-timer (:id timer)))

(defn show-modal! [modal]
  (swap! state assoc :modal modal))


(defn close-modal! []
  (swap! state assoc :modal nil))
