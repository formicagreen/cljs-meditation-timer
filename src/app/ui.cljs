(ns app.ui
  (:require
   [reagent.core :as r]
   [reagent.dom :as dom]
   [app.domain :as d]
   [cljs.spec.alpha :as s]
   [cljs.spec.test.alpha :as stest]
   [app.spec :as spec]
   ["@capacitor/preferences" :as prefs]
   ["@heroicons/react/20/solid" :as icon-sm-solid]))


(defonce state (r/atom (d/initial-state)))


(defn gen-timer-name []
  (str "Timer " (inc (count (:timers @state)))))


(defn current-timer []
  (d/by-id (:timers @state) (:current-timer @state)))


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


(defn add-and-edit-timer! []
  (let [name  (str "Timer " (inc (count (:timers @state))))
        timer (d/timer name)]
    (swap! state
           (fn [s] (-> s
                       (update :timers conj timer)
                       (assoc :page :edit))))))
(defn page [k body]
  [:div
   {:style {:background "linear-gradient(180deg, hsl(240deg 83% 2%), hsl(265deg 100% 20%))"}
    :class ["w-screen h-screen transition-transform duration-300 flex flex-col
                    overflow-hidden fixed"
            (when (is-above-current-page k) "transform translate-x-full")]}
   body])


(defn edit-timer! [timer]
  (swap! state assoc
         :page :edit
         :current-timer (:id timer)))


(defn show-modal! [modal]
  (swap! state assoc :modal modal))


(defn close-modal! []
  (swap! state assoc :modal nil))


(defn timer-hand [deg id]
  [:div
   {:class "w-[71.5%] aspect-square absolute"
    :key id
    :style {:transform (str "rotate(" (+ 45 deg) "deg)")}}
   [:div
    {:class "bg-stone-50 shadow shadow-amber-400 rounded-full 
             aspect-square -translate-x-1/2 -translate-y-1/2"
     :style {:width "22%"}}]])


(defn timer-circle [timer progress]
  [:div
   {:class "rounded-full border-2 border-stone-50 w-full 
            aspect-square relative grid place-items-center"}
   (for [step (:steps timer)]
     ^{:key (:id step)}
     (timer-hand
      (d/duration-inclusive->degrees timer step)
      (:id step)))
   (when progress
     (timer-hand
      (d/progress->degrees timer progress)
      :progress))])


(defn detect-overflow [element]
  (let [scroll-height (.-scrollHeight element)
        client-height (.-clientHeight element)
        scroll-top (.-scrollTop element)]
    {:top (> scroll-top 0)
     :bottom (> scroll-height (+ scroll-top client-height))}))


(defn index-page []
  (page
   :index
   ; header
   [:<>
    [:div {:class "w-full flex p-6 justify-end z-20 shadow"}
     [:button
      {:on-click #(show-modal! :about)}
      "About"]]
   ; main area
    [:div {:class "flex-grow"}
     (for [timer (:timers @state)]
       ^{:key (:id timer)}
       [:button
        {:class "p-6 flex w-full text-2xl items-center gap-6 border-b first:border-t border-stone-600 border-opacity-75"
         :on-click #(edit-timer! timer)}
        [:div.w-12 (timer-circle timer nil)]
        [:div.flex-grow.font-semibold (:name timer)]
        [:div (d/duration-min timer) " min"]])]
  ; footer
    [:div {:class "flex justify-center w-full bottom-0 p-6 shadow-up"}
     [:button
      {:on-click #(add-and-edit-timer!)
       :class "font-semibold rounded-full border-2 border-stone-50 
              flex gap-1 px-4 py-2 z-20 items-center"}
      [:> icon-sm-solid/PlusIcon {:class "h-6 w-6"}]
      "Add timer"]]]))


(comment 
  (detect-overflow (js/document.querySelector ".overflow-scroll"))
  )

(defn edit-page []
  (page
   :edit
   ; header
   [:<>
    [:div#edit-header
     {:class "fixed w-full flex p-6 justify-start z-20"}
     [:button
      {:on-click #(swap! state assoc :page :index)
       :class "flex gap-1 items-center"}
      [:> icon-sm-solid/ChevronLeftIcon {:class "h-6 w-6"}]
      "Back"]]
    (when (:current-timer @state)
     ; steps
      [:div
       {:class "p-6"}
       (for [step (:steps (current-timer))]
         ^{:key (:id step)}
         [:div
          {:class "p-6 flex w-full text-2xl items-center gap-6 
                  border-b first:border-t border-stone-600 border-opacity-75"}
          [:input
           {:class "text-stone-700 w-12 px-4 py-2"
            :value (:duration step)
            :on-change #(js/console.log "heyf")}]])])]))


(defn modal [open & body]
  [:div
   {:class ["fixed inset-0 z-10 bg-black bg-opacity-50
             grid place-items-center transition-opacity"
            (when-not open "opacity-0 pointer-events-none")]}
   [:div
    {:class "fixed inset-0"
     :on-click #(close-modal!)}]
   (into
    [:div
     {:class "bg-white rounded-lg text-stone-800 p-6 
              text-center flex flex-col items-center z-20"}]
    body)])


(defn about-modal []
  [modal
   (= :about (:modal @state))
   [:h2.text-4xl.mb-6 "BĪJA"]
   [:p "Meditation sequence timer"]
   [:p.mb-6 "© 2021 Dag Norberg"]
   [:button
    {:class "rounded-full bg-stone-100 px-4 py-2 
                flex gap-1 items-center"
     :on-click #(close-modal!)}
    [:> icon-sm-solid/XMarkIcon {:class "h-6 w-6"}]
    "Close"]])


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
  (into [:details
         {:class "mb-2"}
         [:summary
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
   {:class "fixed bottom-0 text-sm bg-black bg-opacity-50 z-50 p-4 overflow-auto"}
   (cond
     (map? @state) [render-map @state]
     (sequential? @state) [render-seq @state]
     :else (pr-str @state))])


(defn wrapper-component [body]
  [:div [body]])


(defn app []
  [:div
   {:class "text-lg text-stone-50 overflow-hidden"}
   (when (:debug @state) [debugger])
   [index-page]
   [edit-page]
   [about-modal]])


(comment
  (swap! state assoc :page :edit)
  (swap! state assoc :page :index)
  (reset! state (d/initial-state))
  (s/explain-data :spec/state @state)
  (show-modal! :about)
  (close-modal!)
  (swap! state assoc :debug true)
  (swap! state assoc :debug false)
  (d/by-id (:timers @state) (:current-timer @state))
  (add-and-edit-timer!)
  @state
  )

