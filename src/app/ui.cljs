(ns app.ui
  (:require
   [app.core :as core]
   [app.state :as state]
   ["@heroicons/react/20/solid" :as icon-sm-solid]))


(comment 
  @state/state
  )


(def page-order [:index :edit :run])


(defn is-above-page [page1 page2]
  (> (.indexOf page-order page1) (.indexOf page-order page2)))


(defn is-above-current-page [page]
  (is-above-page page (:page @state/state)))


(comment
  (is-above-page :edit :index)
  (is-above-page :index :run)
  (is-above-current-page :edit))

#_(defn get-height [selector]
    (str (.-clientHeight (js/document.querySelector selector)) "px"))


(defn page [k body]
  [:div
   {:style {:background "url(noise-random-lighter.png), linear-gradient(rgb(16 12 40), rgb(59, 55, 113))"}
    :class ["w-screen h-screen transition-transform duration-300 flex flex-col overflow-hidden fixed bg-blend-overlay"
            (when (is-above-current-page k) "transform translate-x-full")]}
   body])


(def timer-radius 80)


(def timer-center 100)


(defn timer-svg-step [angle step-radius class]
  (let [rad (* (- angle 90) (/ Math/PI 180))
        x (+ timer-center (* timer-radius (Math/cos rad)))
        y (+ timer-center (* timer-radius (Math/sin rad)))]
    [:circle {:cx x :cy y :r step-radius :class class}]))


(defn timer-svg
  [& {:keys [timer stroke-width step-radius elapsed]
      :or {elapsed nil}}]
  [:svg {:viewBox "0 0 200 200" :class "w-full h-auto"}
             ;; Main circle
   [:circle {:cx timer-center :cy timer-center :r timer-radius :class "stroke-current fill-none" :stroke-width stroke-width}]
             ;; Smaller circles along the circumference
   (for [step (:steps timer)]
     ^{:key (:id step)}
     [timer-svg-step (core/step->angle timer step) step-radius "fill-current"])
   (when elapsed
     [timer-svg-step (core/time->angle timer (core/ms->min elapsed)) step-radius "fill-orange-500"])])


#_(defn detect-overflow [element]
    (let [scroll-height (.-scrollHeight element)
          client-height (.-clientHeight element)          scroll-top (.-scrollTop element)]
      {:top (> scroll-top 0)
       :bottom (> scroll-height (+ scroll-top client-height))}))


(defn index-page []
  [page :index
      ; header
   [:<>
    [:div {:class "w-full flex p-6 justify-end z-20 shadow"}
     [:button
      {:on-click #(state/show-modal! :about)}
      "About"]]
      ; main area
    [:div {:class "flex-grow overflow-y-scroll overflow-x-hidden"}
     (for [timer (:timers @state/state)]
       ^{:key (:id timer)}
       [:button
        {:class "p-6 grid w-full text-2xl items-center gap-4 
                    border-b first:border-t border-stone-700 border-opacity-25"
         :style {:grid-template-columns "3rem 2fr auto"}
         :on-click #(state/edit-timer! timer)}
        [:div [timer-svg {:timer timer :stroke-width 2 :step-radius 2}]]
        [:div
         {:class "font-medium overflow-ellipsis overflow-hidden whitespace-nowrap"}
         (:name timer)]
        [:div
         {:class "whitespace-nowrap w-min"}
         (core/duration timer) " min"]])]
     ; footer
    [:div {:class "flex justify-center w-full bottom-0 p-6"}
     [:button
      {:on-click #(state/add-and-edit-timer!)
       :class "font-semibold rounded-full border-2 border-stone-50 
                 flex gap-1 px-4 py-2 z-20 items-center"}
      [:> icon-sm-solid/PlusIcon {:class "h-6 w-6"}]
      "Add timer"]]]])


(defn back-button [f]
  [:button
   {:on-click f
    :class "flex gap-1 items-center"}
   [:> icon-sm-solid/ChevronLeftIcon {:class "h-6 w-6"}]
   "Back"])


(defn edit-page []
  [page
   :edit
      ; header
   (when-some [timer (state/current-timer)]
     [:<>
      [:div {:class "p-6 flex gap-6 flex-col"}
       [:div
        {:class "flex justify-start"}
        [back-button (fn [] (swap! state/state assoc :page :index))]]
       [:input
        {:value (:name timer)
         :on-change
         #(swap! state/state (fn [s] (core/rename-timer s timer (-> % .-target .-value))))
         :class "text-stone-700 py-2 px-4 rounded shadow-inner shadow-stone-500/75"}]
       [:button.btn.w-full
        {:on-click #(swap! state/state assoc :page :run)}
        [:> icon-sm-solid/PlayIcon {:class "h-6 w-6"}]
        "Run"]]
      ; steps
      [:div
       {:class "flex-grow overflow-y-scroll"}
       (for [step (:steps timer)]
         ^{:key (:id step)}
         [:div
          {:class "p-6 flex w-full gap-6 items-center
                        border-b first:border-t border-stone-700 border-opacity-25"}
          [:div.flex-grow
           [:input
            {:class "text-stone-700 w-12 py-2 mr-6 rounded shadow-inner text-center shadow-stone-500/75"
             :type "number"
             :pattern "[0-9]*"
             :on-change
             #(swap! state/state (fn [s] (core/update-duration s timer step (-> % .-target .-value int))))
             :value (:duration step)}] "min"]

          [:button.p-2.transition-opacity
           {:class (when (-> timer :steps count (< 2))
                     "opacity-0 pointer-events-none") ; don't show delete button if there is only one step
            :on-click #(swap! state/state (fn [s] (core/delete-step s timer step)))}
           [:> icon-sm-solid/XMarkIcon {:class "h-6 w-6"}]]])]
      ; footer
      [:div {:class "grid grid-cols-2 gap-6 p-6"}
       [:button.btn
        {:on-click #(state/show-modal! :delete-timer)}
        [:> icon-sm-solid/XMarkIcon {:class "h-6 w-6"}]
        "Delete"]
       [:button.btn
        {:on-click #(swap! state/state (fn [s] (core/add-step s timer)))}
        [:> icon-sm-solid/PlusIcon {:class "h-6 w-6"}]
        "Add step"]]])])


(comment
  (:current-timer @state/state))


(defn modal [k body]
  [:div
   {:class ["fixed inset-0 z-10 bg-black bg-opacity-50
             grid place-items-center transition-opacity"
            (when-not (= k (:modal @state/state)) "opacity-0 pointer-events-none")]}
   [:div
    {:class "fixed inset-0"
     :on-click #(state/close-modal!)}]
   [:div
    {:class "bg-white rounded-lg text-stone-800 p-6 
              text-center flex flex-col items-center z-20"}
    body]])


(defn about-modal []
  [modal :about
   [:<>
    [:h2.text-4xl.mb-6 "BĪJA"]
    [:p "Meditation sequence timer"]
    [:p.mb-6 "© 2023 Dag Norberg"]
    [:button
     {:class "rounded-full bg-stone-100 px-4 py-2 
                flex gap-1 items-center"
      :on-click #(state/close-modal!)}
     [:> icon-sm-solid/XMarkIcon {:class "h-6 w-6"}]
     "Close"]]])


(defn delete-timer-modal []
  [modal :delete-timer
   [:<>
    [:h2.text-2xl.mb-6 "Delete timer?"]
    [:div {:class "flex gap-6"}
     [:button.btn
      {:on-click #(state/close-modal!)}
      [:> icon-sm-solid/XMarkIcon {:class "h-6 w-6"}]
      "Cancel"]
     [:button.btn
      {:on-click #(swap! state/state (fn [s] (core/delete-timer s (state/current-timer))))
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
     (map? @state/state) [render-map @state/state]
     (sequential? @state/state) [render-seq @state/state]
     :else (pr-str @state/state))])


(defn monospaced-time
  "Separates the time into digits and wraps each digit in a div with a fixed width"
  [ms]
  [:div
   {:class "absolute text-7xl flex"}
   (for [[i c] (map-indexed vector (str (core/format-time ms)))]
     (let [width (if (= c ":") "w-6" "w-12")]
       ^{:key i}
       [:div {:class ["text-center" width]} c]))])


(defn run-page []
  [page :run
   (when-let [timer (state/current-timer)]
     [:<>
      [:div
       {:class "p-6 flex justify-start"}
       [back-button (fn []
                      (state/stop!)
                      (swap! state/state assoc :page :edit))]]
      [:div
       {:class "grid place-items-center flex-grow relative"}
       [timer-svg {:timer timer 
                   :stroke-width 2 
                   :step-radius 5 
                   :elapsed (:elapsed @state/state)}]
       [monospaced-time (:elapsed @state/state)]]
      [:div
       {:class "p-6 grid gap-6 grid-cols-2"}
       [:button.btn
        {:on-click #(state/stop!)}
        [:> icon-sm-solid/StopIcon {:class "h-6 w-6"}]
        "Stop"]
       (case (core/play-state @state/state)
         :running
         [:button.btn
          {:on-click #(state/pause!)}
          [:> icon-sm-solid/PauseIcon {:class "h-6 w-6"}]
          "Pause"]
         :paused
         [:button.btn
          {:on-click #(state/resume!)}
          [:> icon-sm-solid/PlayIcon {:class "h-6 w-6"}]
          "Resume"]
         :stopped
         [:button.btn
          {:on-click #(state/start! timer)}
          [:> icon-sm-solid/PlayIcon {:class "h-6 w-6"}]
          "Start"])]])])


(defn app []
  [:div
   {:class "text-lg text-stone-50 overflow-hidden"}
   (when (:debug @state/state) [debugger])
   [index-page]
   [edit-page]
   [run-page]
   [about-modal]
   [delete-timer-modal]])