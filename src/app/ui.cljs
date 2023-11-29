(ns app.ui
  (:require
   [app.core :as core]
   [app.state :as state]
   [reagent.core :as r]
   ["@capacitor/core" :refer [Capacitor]]
   ["@heroicons/react/20/solid" :as icon-sm-solid]))


(comment
  @state/session
  (.getPlatform Capacitor))


(def page-order [:index :edit :run])


(defn is-above-page [page1 page2]
  (> (.indexOf page-order page1) (.indexOf page-order page2)))


(comment
  state/session
  (is-above-page :edit :index)
  (is-above-page :index :run))


(defn page [k body]
  [:div
   {:style {:padding-top "env(safe-area-inset-top)"
            :background "rgb(0, 50, 130)"}
    :class ["w-screen h-screen transition-transform duration-300 flex flex-col overflow-hidden fixed bg-blend-overlay"
            ; move offscreen to the right if above current page
            (when (is-above-page k (:page @state/session)) "transform translate-x-full")]}
   body])


(def timer-radius 80)


(def timer-center 100)


(defn timer-svg-step [angle step-radius other-props]
  (let [rad (* (- angle 90) (/ Math/PI 180))
        x (+ timer-center (* timer-radius (Math/cos rad)))
        y (+ timer-center (* timer-radius (Math/sin rad)))]
    [:circle (merge {:cx x :cy y :r step-radius} other-props)]))


(defn timer-svg
  [& {:keys [timer stroke-width step-radius elapsed]
      :or {elapsed nil}}]
  [:svg {:viewBox "0 0 200 200"
         :class "w-full"
         :style {:max-height "75vh"}}
   [:defs
    [:radialGradient {:id "gradient" :x1 0 :y1 0 :x2 0 :y2 "100%"}
     [:stop {:offset "0%" :stop-color "yellow"}]
     [:stop {:offset "100%" :stop-color "darkorange"}]]]
   
   ;; Main circle
   [:circle {:cx           timer-center
             :cy           timer-center
             :r            timer-radius
             :class        "stroke-current fill-none"
             :stroke-width stroke-width}]
   
   ;; Smaller circles along the circumference
   (for [step (:steps timer)]
     ^{:key (:id step)}
     [timer-svg-step
      (core/step->angle timer step)
      step-radius
      {:fill "white"}])
   
   (when elapsed
     [timer-svg-step
      (core/time->angle timer (core/ms->min elapsed))
      step-radius
      {:fill "url(#gradient)"}])])


(defn overflow-above? [k]
  (> (get-in @state/scroll-areas [k :scroll-top]) 0))


(defn overflow-below? [k]
  (let [element (get-in @state/scroll-areas [k :element])]
    (> (- (:scroll-height element) (:client-height element))
       (:scroll-top element))))


(defn shadow-element
  [visible? position]
  [:div
   {:class ["h-6 w-full from-blue-900/50 to-transparent pointer-events-none transition-opacity absolute"
            (if visible? "opacity-100" "opacity-0")
            (when (= position :top) "top-0  bg-gradient-to-b")
            (when (= position :bottom) "bottom-0 bg-gradient-to-t")]}])


(comment
  @state/scroll-areas)

(defn scrollable
  "This is an insanely complicated component that:
   1. Shows a shadow at the top/bottom of the scrollable area to indicate that there is more content above/below.
   2. Maintains scroll position between renders.
   It's a lot of work, but will people notice the difference? Probably not.
   What makes this so difficult is that React lacks fine-grained reactivity. It would be trivial to implement in vanilla JS.
   So all of the state that this component needs to keep track of is stored in a global atom.
   You might be able to achieve something similar by carefully managing the lifecycle of the scrollable area.
   But that also seems like a lot of work and kind of brittle."
  [key body]
  (let [div-ref (r/atom nil)
        prev-body (r/atom nil)
        get-scroll-pos (fn [] (get-in @state/scroll-areas [key :scroll]))
        get-client-height (fn [] (get-in @state/scroll-areas [key :client-height]))
        get-scroll-height (fn [] (get-in @state/scroll-areas [key :scroll-height]))
        set-scroll-pos! (fn [pos] (swap! state/scroll-areas assoc-in [key :scroll] pos))
        set-height! (fn []
                      (swap! state/scroll-areas
                             #(-> %
                                  (assoc-in [key :client-height] (.-clientHeight @div-ref))
                                  (assoc-in [key :scroll-height] (.-scrollHeight @div-ref)))))
        overflow-above? (fn [] (and (> (get-scroll-pos) 0) (> (get-scroll-height) (get-client-height))))
        overflow-below? (fn [] (> (- (get-scroll-height) (get-client-height))
                                  (get-scroll-pos)))]
    [(r/create-class
      {:component-did-mount (fn []
                              (set-height!)
                              (set! (.-scrollTop @div-ref) (get-scroll-pos)))
       :component-did-update (fn [] (when (not= @prev-body body)
                                      (set-height!)
                                      (reset! prev-body body)
                                      (set! (.-scrollTop @div-ref) (get-scroll-pos))))
       :reagent-render (fn []
                         [:div
                          {:class "flex flex-grow overflow-auto w-full flex-col relative"}
                          [shadow-element (overflow-above?) :top]
                          [:div
                           {:ref #(reset! div-ref %)
                            :on-scroll (fn [] (set-scroll-pos! (.-scrollTop @div-ref)))
                            :class "flex-grow overflow-auto h-full w-full"}
                           body]
                          [shadow-element (overflow-below?) :bottom]])})]))



(comment
  @state/session)


(defn index-page []
  [page :index
      ; header
   [:<>
    [:div {:class "w-full flex p-6 justify-end z-20"}
     [:button.font-medium
      {:on-click #(state/show-modal! :about)}
      "About"]]
      ; main area
    [scrollable :timers
     (for [timer (:timers @state/persistent)]
       ^{:key (:id timer)}
       [:button
        {:class "timer p-6 grid w-full text-2xl items-center gap-4"
         :style {:grid-template-columns "3rem 2fr auto"}
         :on-click #(state/edit-timer! timer)}
        [:div [timer-svg {:timer timer :stroke-width 10 :step-radius 16}]]
        [:div
         {:class "font-medium overflow-ellipsis overflow-hidden whitespace-nowrap"}
         (:name timer)]
        [:div
         {:class "whitespace-nowrap w-min"}
         (core/full-duration timer) " min"]])]
     ; footer
    [:div {:class "flex justify-center w-full bottom-0 p-6"}
     [:button
      {:on-click (fn [] (state/add-and-edit-timer!)
                   ; scroll down so that next time we come back to the index page we see the new timer
                   (js/setTimeout #(.focus (.querySelector js/document  ".timer:last-child")) 300))
       :class "font-semibold rounded-full border-2  
                 flex gap-1 px-4 py-2 z-20 items-center"}
      [:> icon-sm-solid/PlusIcon {:class "h-6 w-6"}]
      "Add timer"]]]])

(comment
  (def timer (-> @state/persistent :timers first)))


(defn back-button [f]
  [:button.font-medium
   {:on-click f
    :class "flex gap-1 items-center font-medium"}
   [:> icon-sm-solid/ChevronLeftIcon {:class "h-6 w-6"}]
   "Back"])


(defn duration-input
  "This needs to be a reagent form-2 component because it has its own intermediate state before validation."
  [timer step]
  (let [input-val (r/atom (:duration step))]
    (println "rendering duration input")
    (fn []
      [:div.flex.items-center.gap-4
       [:div.p-2
        {:on-click #(state/persist!
                     (fn [s] (core/dec-duration s timer step)))}
        [:> icon-sm-solid/MinusIcon
         {:class "h-6 w-6"}]]
       [:div.relative.w-12.inline-block.text-center
        [:input
         {:class "text-stone-700 w-full py-2 mr-6 rounded shadow-inner text-center shadow-stone-500/75"
          :type "number"
          :id (:id step)
          :pattern "[0-9]*"
          :tabIndex 1
          :value @input-val
          :on-change #(reset! input-val (-> % .-target .-value))
          :on-blur
          (fn [e]
            (let [new-state (state/persist!
                             #(core/update-duration % timer step (-> e .-target .-value int)))]
              (reset! input-val (:duration (core/get-step new-state timer step)))))}]
        [:div
      ; this div is here to catch click events and focus the input that is below it
      ; this is to make sure the cursor is always placed after the last digit
      ; (which is not the case if you click on the input directly)
      ; the setTimeout is a hack to deal with react re-rendering too much
      ; i.e. on blur you try click another input but it immediately re-renders and the click is lost
      ; scrollIntoView is there to deal with the janky ios keyboard moving the input out of view sometimes. it's not the best solution
         {:class "absolute inset-0"
          :on-click (fn [e]
                      (let [get-el #(-> js/document (.getElementById (:id step)))]
                        (js/setTimeout #(.focus (get-el)) 10)
                        (js/setTimeout #((.scrollIntoView (get-el) #js {:behavior "smooth"})) 200)))}]]
       [:div.p-2
        {:on-click #(state/persist!
                     (fn [s] (core/inc-duration s timer step)))}
        [:> icon-sm-solid/PlusIcon
         {:class "h-6 w-6"}]]])))


(defn edit-page []
  [page
   :edit
      ; header
   (when-some [timer (state/current-timer)]
     [:<>
      (println "Rendering edit page")
      [:div {:class "p-6 flex gap-6 flex-col"}
       [:div
        {:class "flex justify-start"}
        [back-button (fn [] (state/swap-page! :index))]]
       [:input
        {:value (:name timer)
         :spellCheck "false"
         :on-change
         #(state/persist! (fn [s] (core/rename-timer s timer (-> % .-target .-value))))
         :class "text-stone-700 py-2 px-4 rounded shadow-inner shadow-stone-500/75"}]
       [:button.btn.w-full
        {:on-click (fn []
                     (state/swap-page! :run)
                     (state/start! timer))}
        [:> icon-sm-solid/PlayIcon {:class "h-6 w-6"}]
        "Start timer"]]
      ; steps
      [scrollable :steps
       (for [step (:steps timer)]
         ^{:key (:id step)}
         [:div
          {:class "step px-6 py-3 flex w-full items-center"}
          [:div.flex-grow [duration-input timer step]]
          [:div.p-2
           {:class (when (-> timer :steps count (< 2)) ; don't show delete button if there is only one step
                     "opacity-25 pointer-events-none")
            :on-click
            #(state/persist!
              (fn [s] (core/delete-step s timer step)))}
           [:> icon-sm-solid/XMarkIcon {:class "h-6 w-6"}]]])]
      ; footer
      [:div {:class "grid grid-cols-2 gap-6 p-6"}
       [:button.btn
        {:on-click (fn []
                     (when (js/confirm "Delete timer?")
                       (state/swap-page! :index)
                       (js/setTimeout #(state/persist! core/delete-timer timer) 300)))}
        [:> icon-sm-solid/XMarkIcon {:class "h-6 w-6"}]
        "Delete" [:span {:class "hidden md:inline"} "timer"] ; both words won't fit on small screens
        ]
       [:button.btn
        {:on-click (fn [e]
                     (state/persist! (fn [s] (core/add-step s timer)))
                     ; when dom has updated, focus last timer in list
                     (js/setTimeout #(.focus (.querySelector js/document  ".step:last-child input")) 10))}
        [:> icon-sm-solid/PlusIcon {:class "h-6 w-6"}]
        "Add step"]]])])


(comment
  (:current-timer @state/session))


(defn modal [k body]
  [:div
   {:class ["fixed inset-0 z-10 bg-black bg-opacity-50
             grid place-items-center transition-opacity"
            (when-not (= k (:modal @state/session)) "opacity-0 pointer-events-none")]}
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
    [:h2.text-4xl.mb-6 core/app-name]
    [:p "Simple meditation timer"]
    [:p.mb-6 "© 2023 Dag Norberg"]
    [:p.mb-6 "v1.2"]
    [:button
     {:class "rounded-full bg-stone-100 px-4 py-2 
                flex gap-1 items-center"
      :on-click #(state/close-modal!)}
     [:> icon-sm-solid/XMarkIcon {:class "h-6 w-6"}]
     "Close"]
    [:button
     {:on-click #(swap! state/session update :debug not)
      :class "text-white opacity-10 absolute bottom-0 left-0 p-4"}
     "Debug"]]])


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
      {:on-click
       #(state/persist!
         (fn [s] (-> s
                     (core/delete-timer (state/current-timer))
                     (assoc :page :index)
                     (assoc :modal nil))))
       :class "bg-red-600 text-white"}
      [:> icon-sm-solid/TrashIcon {:class "h-6 w-6"}]
      "Delete"]]]])


(declare render-seq)


(defn render-map [m]
  (into [:details
         {:class "mb-2"}
         [:summary
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


(defn debugger [debug-info]
  [:div
   {:class "fixed bottom-0 right-0 h-[50vh] text-sm bg-black bg-opacity-50 z-50 p-4 overflow-scroll max-h-screen w-screen"}
   (cond
     (map? debug-info) [render-map debug-info]
     (sequential? debug-info) [render-seq debug-info]
     :else (pr-str debug-info))])


(defn monospaced-time
  "Separates the time into digits and wraps each digit in a div with a fixed width"
  [ms font-size]
  [:div.flex.leading-none.justify-center
   {:style {:font-size (str font-size "px")}}
   (for [[i c] (map-indexed vector (str (core/format-time ms)))]
     (let [width (if (= c ":") (* font-size 0.3) (* font-size 0.7))]
       ^{:key i}
       [:div.text-center
        {:style {:width (str width "px")}}
        c]))])


(defn run-page []
  [page :run
   (when-let [timer (state/current-timer)]
     [:<>
      [:div
       {:class "p-6 flex justify-start"}
       [back-button (fn []
                      (state/stop!)
                      (state/swap-page! :edit))]]
      [:div
       {:class "grid place-items-center flex-grow relative"}
       [timer-svg {:timer timer
                   :stroke-width 2
                   :step-radius 5
                   :elapsed @state/elapsed}]
       [:div.absolute.text-center
        (if (-> timer :steps count (> 1))
          (let [current-step (state/current-step)]
            [:<>
             [:div.pb-4.opacity-75 "Step " (inc (core/step-index timer current-step)) " of " (count (:steps timer))]
             [monospaced-time (state/remaining-current-step) "70"]
             [:div.pt-6.opacity-75
              [monospaced-time (state/remaining-total) "20"]]])
          [monospaced-time (state/remaining-total) "70"])]]
      [:div
       {:class "p-6 grid gap-6 grid-cols-2"}
       [:button.btn
        {:on-click #(state/stop!)}
        [:> icon-sm-solid/StopIcon {:class "h-6 w-6"}]
        "Stop"]
       (case (core/play-state @state/session)
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


(comment
  (def timer (state/current-timer))
  (def current-step (state/current-step)))

(defn app []
  [:div
   {:class "text-lg text-white overflow-hidden"}
   (println "rendering app")
   (when (:debug @state/session) [debugger (reverse (:log @state/persistent))])
   [index-page]
   [edit-page]
   [run-page]
   [about-modal]])

(comment
  (state/reset-state!)
  (swap! state/session assoc :debug true)
  (swap! state/session assoc :debug false)
  @state/elapsed)