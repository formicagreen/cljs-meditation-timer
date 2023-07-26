(ns app.main
  (:require
   [reagent.dom :as rdom]
   [app.ui :as ui]))

(defn main! []
  (println "main!")
  (rdom/render [ui/app]
            (.getElementById js/document "app")))

(defn ^:dev/after-load reload! []
  (println "reload!")
  (main!))