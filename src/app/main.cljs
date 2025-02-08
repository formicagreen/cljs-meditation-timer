(ns app.main
  (:require
   [reagent.dom :as rdom]
   [app.ui :as ui]
   [app.state :as state]
   ["@capacitor/preferences" :refer [Preferences]]
   ["@capacitor/app" :refer [App]]
   ["@capacitor/local-notifications" :refer [LocalNotifications]]
   cljs.reader))


(defn main! []
  (println "main!")
  ; load stored data
  (->
   (.get Preferences #js {:key "storage"})
   (.then (fn [prefs]
            (when-let [edn (.-value prefs)]
              (reset! state/persistent (cljs.reader/read-string edn)))))
   (.catch (fn [err] (println err))))
  ; request notification permissions
  (.requestPermissions LocalNotifications)
  ; listen to app foreground/background events
  (.addListener App "appStateChange" #(state/handle-app-state-change! %))
  ; fix to prevent sleep mode when timer is running
  (state/poll-keepawake!)
  ; render app
  (rdom/render [ui/app] (.getElementById js/document "app")))


(defn ^:dev/after-load reload! []
  (println "reload!")
  ; remove appStateChange listener
  (.removeAllListeners App)
  ; run main! again
  (main!))

(comment
  @state/persistent
  (js/alert "hey!") 
  (println "hey")
  )