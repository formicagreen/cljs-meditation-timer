(ns app.ui
  (:require
   [reagent.core :as r]
   [reagent.dom :as dom]
   [app.domain :as d]
   [cljs.spec.alpha :as s]
   [cljs.spec.test.alpha :as stest]
   [app.spec :as spec]
   ["@capacitor/core" :refer (Capacitor)]
   ["@capacitor/preferences" :refer (Preferences)]
   ["@heroicons/react/20/solid" :as icon-sm-solid]))


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


(defonce state (r/atom (d/initial-state)))


(defonce storage
  (r/atom (js/JSON.parse nil #_(prefs.get "storage"))))


(defn reset-state! []
  (reset! state (d/initial-state)))


(defn current-timer []
  (d/where (:timers @state) :id (:current-timer @state)))


(def page-order [:index :edit :run])


(defn is-above-page [page1 page2]
  (> (.indexOf page-order page1) (.indexOf page-order page2)))


(defn is-above-current-page [page]
  (is-above-page page (:page @state)))


(defn get-height [selector]
  (str (.-clientHeight (js/document.querySelector selector)) "px"))


(comment
  (is-above-page :edit :index)
  (is-above-page :index :run)
  (is-above-current-page :edit))



(defn first-timer []
  (first (:timers @state)))


(defn first-step-of-first-timer []
  (first (:steps (first-timer))))


(defn add-and-edit-timer! []
  (let [name  (str "Timer " (inc (count (:timers @state))))
        timer (d/timer name)]
    (swap! state
           (fn [s] (-> s
                       (update :timers conj timer)
                       (assoc :current-timer (:id timer))
                       (assoc :page :edit))))))


(defn page [k body]
  [:div
   {:style {:background "linear-gradient(180deg, hsl(240deg 83% 2%), hsl(265deg 100% 20%))"}
    :class ["w-screen h-screen transition-transform duration-300 flex flex-col
                    overflow-hidden fixed"
            (when (is-above-current-page k) "transform translate-x-full")]}
   body])

#_(defn set-state! [f]
    (swap! state f)
    (prefs.set "state" @state)
    (println prefs))


(defn edit-timer! [timer]
  (swap! state assoc
         :page :edit
         :current-timer (:id timer)))


(defn show-modal! [modal]
  (swap! state assoc :modal modal))


(defn close-modal! []
  (swap! state assoc :modal nil))


(defn timer-svg [timer stroke-width step-radius]
  (let [radius 80
        center 100]
    [:svg {:viewBox "0 0 200 200" :class "w-full h-auto"}
           ;; Main circle
     [:circle {:cx center :cy center :r radius :class "stroke-current fill-none" :stroke-width stroke-width}]
           ;; Smaller circles along the circumference
     (for [step (:steps timer)]
       (let [angle (d/step->angle timer step)
             rad (* (- angle 90) (/ Math/PI 180))
             x (+ center (* radius (Math/cos rad)))
             y (+ center (* radius (Math/sin rad)))]
         ^{:key (:id step)}
         [:circle {:cx x :cy y :r step-radius :class "fill-current shadow-amber-400"}]))]))


#_(defn detect-overflow [element]
    (let [scroll-height (.-scrollHeight element)
          client-height (.-clientHeight element)
          scroll-top (.-scrollTop element)]
      {:top (> scroll-top 0)
       :bottom (> scroll-height (+ scroll-top client-height))}))


(defn index-page []
  [page :index
      ; header
   [:<>
    [:div {:class "w-full flex p-6 justify-end z-20 shadow"}
     [:button
      {:on-click #(show-modal! :about)}
      "About"]]
      ; main area
    [:div {:class "flex-grow overflow-y-scroll overflow-x-hidden"}
     (for [timer (:timers @state)]
       ^{:key (:id timer)}
       [:button
        {:class "p-6 grid w-full text-2xl items-center gap-6 
                    border-b first:border-t border-stone-600 border-opacity-75"
         :style {:grid-template-columns "3rem 2fr auto"}
         :on-click #(edit-timer! timer)}
        [:div [timer-svg timer 10 15]]
        [:div
         {:class "font-semibold overflow-ellipsis overflow-hidden whitespace-nowrap"}
         (:name timer)]
        [:div
         {:class "whitespace-nowrap w-min"}
         (d/duration timer) " min"]])]
     ; footer
    [:div {:class "flex justify-center w-full bottom-0 p-6 shadow-up"}
     [:button
      {:on-click #(add-and-edit-timer!)
       :class "font-semibold rounded-full border-2 border-stone-50 
                 flex gap-1 px-4 py-2 z-20 items-center"}
      [:> icon-sm-solid/PlusIcon {:class "h-6 w-6"}]
      "Add timer"]]]])

(defn back-button [page]
  [:button
   {:on-click #(swap! state assoc :page page)
    :class "flex gap-1 items-center"}
   [:> icon-sm-solid/ChevronLeftIcon {:class "h-6 w-6"}]
   "Back"])

(defn edit-page []
  [page
   :edit
      ; header
   (when-some [timer (current-timer)]
     [:<>
      [:div {:class "p-6 flex gap-6 flex-col"}
       [:div
        {:class "flex justify-start"}
        [back-button :index]]
       [:input
        {:value (:name timer)
         :on-change
         #(swap! state (fn [s] (d/rename-timer s timer (-> % .-target .-value))))
         :class "text-stone-700 py-2 px-4 rounded shadow-inner shadow-stone-500/75"}]
       [:button.btn.w-full
        {:on-click #(swap! state assoc :page :run)}
        [:> icon-sm-solid/PlayIcon {:class "h-6 w-6"}]
        "Run"]]
      ; steps
      [:div
       {:class "flex-grow overflow-y-scroll"}
       (for [step (:steps timer)]
         ^{:key (:id step)}
         [:div
          {:class "p-6 flex w-full gap-6 items-center
                        border-b first:border-t border-stone-600 border-opacity-75"}
          [:div.flex-grow
           [:input
            {:class "text-stone-700 w-12 py-2 mr-6 rounded shadow-inner text-center shadow-stone-500/75"
             :type "number"
             :pattern "[0-9]*"
             :on-change
             #(swap! state (fn [s] (d/update-duration s timer step (-> % .-target .-value int))))
             :value (:duration step)}] "min"]

          [:button.p-2.transition-opacity
           {:class (when (-> timer :steps count (< 2))
                     "opacity-0 pointer-events-none") ; don't show delete button if there is only one step
            :on-click #(swap! state (fn [s] (d/delete-step s timer step)))}
           [:> icon-sm-solid/XMarkIcon {:class "h-6 w-6"}]]])]
      ; footer
      [:div {:class "grid grid-cols-2 gap-6 p-6"}
       [:button.btn
        {:on-click #(show-modal! :delete-timer)}
        [:> icon-sm-solid/XMarkIcon {:class "h-6 w-6"}]
        "Delete"]
       [:button.btn
        {:on-click #(swap! state (fn [s] (d/add-step s timer)))}
        [:> icon-sm-solid/PlusIcon {:class "h-6 w-6"}]
        "Add step"]]])])


(comment
  (:current-timer @state))


(defn modal [k body]
  [:div
   {:class ["fixed inset-0 z-10 bg-black bg-opacity-50
             grid place-items-center transition-opacity"
            (when-not (= k (:modal @state)) "opacity-0 pointer-events-none")]}
   [:div
    {:class "fixed inset-0"
     :on-click #(close-modal!)}]
   [:div
    {:class "bg-white rounded-lg text-stone-800 p-6 
              text-center flex flex-col items-center z-20"}
    body]])


(defn about-modal []
  [modal :about
   [:<>
    [:h2.text-4xl.mb-6 "BĪJA"]
    [:p "Meditation sequence timer"]
    [:p.mb-6 "© 2021 Dag Norberg"]
    [:button
     {:class "rounded-full bg-stone-100 px-4 py-2 
                flex gap-1 items-center"
      :on-click #(close-modal!)}
     [:> icon-sm-solid/XMarkIcon {:class "h-6 w-6"}]
     "Close"]]])

(defn delete-timer-modal []
  [modal :delete-timer
   [:<>
    [:h2.text-2xl.mb-6 "Delete timer?"]
    [:div {:class "flex gap-6"}
     [:button.btn
      {:on-click #(close-modal!)}
      [:> icon-sm-solid/XMarkIcon {:class "h-6 w-6"}]
      "Cancel"]
     [:button.btn
      {:on-click #(swap! state (fn [s] (d/delete-timer s (current-timer))))
       :class "bg-red-600 text-white"}
      [:> icon-sm-solid/TrashIcon {:class "h-6 w-6"}]
      "Delete"]]]])


(declare render-seq)


(defn render-map [m]
  (into [:div
         {:class "mb-2"}
         [:div
          {:class "text-xs font-bold cursor-pointer"}
          (str (pr-str (keys m)))]]
        (map (fn [[k v]]
               [:div
                {:class "ml-4"}
                [:strong
                 {:class "text-xs font-semibold"}
                 (pr-str k)]
                (cond
                  (map? v) [render-map v]
                  (sequential? v) [render-seq v]
                  :else (pr-str v))])
             m)))


(defn render-seq [s]
  (into [:div
         {:class "mb-2"}
         [:div
          {:class "text-xs font-bold cursor-pointer"}
          (str (count s) " items")]]
        (map (fn [v]
               [:div
                {:class "ml-4"}
                (cond
                  (map? v) [render-map v]
                  (sequential? v) [render-seq v]
                  :else (pr-str v))])
             s)))


(defn debugger []
  [:div
   {:class "fixed bottom-0 right-0 h-[50vh] text-sm bg-black bg-opacity-50 z-50 p-4 overflow-scroll max-h-screen"}
   (cond
     (map? @state) [render-map @state]
     (sequential? @state) [render-seq @state]
     :else (pr-str @state))])


(defn play-sound! []
  (.play (js/Audio. "singing-bowl-high.mp3")))


(comment
  (play-sound!))


(defn end-step! [step]
  (println "end-step!")
  (play-sound!)
  (swap! state update :timeouts dissoc (:id step)))


(comment
  @state
  (end-step! (first-step-of-first-timer)))


(defn set-timeout! [step timer]
  (let [speedup (or (:speedup @state) 1)
        time-ms (-> (d/duration-inclusive timer step)
                    d/min->ms
                    (/ speedup))
        timeout (js/setTimeout #(end-step! step) time-ms)]
    [(:id step) timeout]))


(defn set-timeouts! [timer]
  (into {} (for [step (:steps timer)] (set-timeout! step timer))))


(defn update-elapsed-time! [] 
  (swap! state assoc :elapsed (-> 
                               (.now js/Date)
                               (- (:started-at @state))
                               (+ (:previous-time @state))))
  (println (:elapsed @state)))


(defn running? []
  (:started-at @state))


(defn animate! [] 
  (when (running?)
    (update-elapsed-time!)
    (js/requestAnimationFrame animate!)))


(defn start! [timer]
  (swap! state assoc
         :started-at (.now js/Date)
         :timeouts (set-timeouts! timer))
  (animate!))


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


(comment 
  (swap! state assoc :current-timer (:id (first-timer)))
  (reset-state!)
  (swap! state assoc :page :run)
  (swap! state assoc :speedup 1000)
  (swap! state assoc :speedup nil)
  (start! (-> @state :timers first))
  (stop!)
  (pause!) 
  (set-timeouts! (-> @state :timers first))
  @state)


(defn run-page []
  [page :run
   [:<>
    [:div
     {:class "p-6 flex justify-start"}
     [back-button :edit]]
    [:div
     {:class "grid place-items-center flex-grow"}
     [timer-svg (current-timer) 2 5]
     [:div
      {:class "font-bold"}
      (-> (:elapsed @state)
          d/ms->min 
          (str " min"))]]
    [:div
     {:class "p-6 grid gap-6 grid-cols-2"}
     [:button.btn 
      {:on-click #(stop!)}
      "Stop"]
     (case (d/play-state @state)
       :running
       [:button.btn
        {:on-click #(pause!)}
        "Pause"]
       :paused
       [:button.btn
        {:on-click #(resume!)}
        "Resume"]
       :stopped
       [:button.btn
        {:on-click #(start! (current-timer))}
        "Start"])]]])


(defn app []
  [:div
   {:class "text-lg text-stone-50 overflow-hidden"}
   (when (:debug @state) [debugger])
   [index-page]
   [edit-page]
   [run-page]
   [about-modal]
   [delete-timer-modal]])


(comment
  (js/alert "hey")
  (reset! state (d/initial-state))
  (swap! state assoc :page :index)
  (swap! state assoc :page :edit)
  (swap! state assoc :page :run)

  (show-modal! :about)
  (show-modal! :delete-timer)
  (close-modal!)
  (swap! state assoc :debug true)
  (swap! state assoc :debug false)
  (d/where (:timers @state) :id (:current-timer @state))
  (add-and-edit-timer!)
  @state)

