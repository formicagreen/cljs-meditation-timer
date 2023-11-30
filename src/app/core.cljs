(ns app.core
  (:require
   [goog.string :as gstring]
   [goog.string.format]
   [cljs.reader :as edn]))


(def app-name "Dhyāna")


(defn step
  "Returns a new step"
  [duration]
  {:id (random-uuid)
   :duration duration})


(defn timer
  "Returns a new timer"
  [name steps]
  {:id (random-uuid)
   :name name
   :steps steps})


(def initial-state
  {:page :index
   :current-timer nil
   :modal nil
   :started-at nil
   :previous-time 0
   :debug? false})


(def initial-storage
  {:timers [(timer "Timer 1" [(step 5)])
            (timer "Timer 2" [(step 5) (step 5)])
            (timer "Timer 3" [(step 5) (step 5) (step 5)])]
   :log []})


(def test-timer (first (:timers initial-storage)))


(def test-step (first (:steps test-timer)))


(defn min->ms [min]
  (* 1000 60 min))


(defn ms->min [ms]
  (/ ms (* 1000 60)))


(defn step-duration [step]
  (:duration step)
  ; (* (/ (:duration step) 60) 10) ; uncomment this line to speed things up
  )


(defn full-duration
  "The full duration of the timer in minutes"
  [timer]
  (->> (:steps timer)
       (map step-duration)
       (reduce +)))

(comment
  (full-duration test-timer))


(defn where
  "Returns the first item in coll where key = val"
  [coll key val]
  (->> coll
       (filter #(= (get % key) val))
       first))


(defn index-where
  "Returns the index of the first item in coll where key = val"
  [coll key val]
  (->> coll
       (keep-indexed #(when (= (get %2 key) val) %1))
       first))


(defn clamp [n vmin vmax]
  (max vmin (min vmax n)))


(defn timer-path [data timer]
  (index-where (:timers data) :id timer))


(defn step-path
  [data timer step]
  (let [timer-idx (index-where (:timers data) :id (:id timer))
        step-idx (index-where (:steps timer) :id (:id step))]
    [:timers timer-idx :steps step-idx]))


(defn get-step [data timer step]
  (let [path (step-path data timer step)]
    (get-in data path)))


(comment
  (step-path initial-storage test-timer test-step))


(defn update-duration
  [data timer step duration]
  (let [path (conj (step-path data timer step) :duration)]
    (assoc-in data path (clamp duration 1 999))))


(defn inc-duration [data timer step]
  (update-duration data timer step (inc (:duration step))))


(defn dec-duration [data timer step]
  (update-duration data timer step (dec (:duration step))))



(defn rename-timer [data timer name]
  (let [timer-idx (index-where (:timers data) :id (:id timer))
        path [:timers timer-idx :name]]
    (assoc-in data path name)))


(defn remove-where [coll val]
  (->> coll
       (remove #(= val %))
       vec))


(defn delete-step [data timer step]
  (let [timer-idx (index-where (:timers data) :id (:id timer))
        path [:timers timer-idx :steps]]
    (update-in data path remove-where step)))


(comment
  (delete-step initial-storage test-timer test-step))


(defn delete-timer [data timer]
  (update data :timers
          (fn [timers] (vec ; prevent lazy weirdness
                        (remove #(= timer %) timers)))))


(comment
  (delete-timer initial-storage test-timer))


(defn add-step [data timer]
  (let [timer-idx (index-where (:timers data) :id (:id timer))
        path [:timers timer-idx :steps]]
    (update-in data path conj (step nil))))


(comment
  (index-where [{:id 1} {:id 2}] :id 2)
  (assoc-in [[1 2 3] 2 3] [0 0] "a"))


(defn step-index [timer step]
  (index-where (:steps timer) :id (:id step)))


(defn step-start-time
  "Returns the duration of the timer up to the given step."
  [timer step]
  (let [index (step-index timer step)
        steps-until (when index (take index (:steps timer)))]
    (->> steps-until
         (map step-duration)
         (reduce +))))


(defn step-end-time
  "Returns the duration of the timer up to and including the given step."
  [timer step]
  (+ (step-duration step)
     (step-start-time timer step)))


(defn is-last-step? [timer step]
  (= (:id step) (:id (last (:steps timer)))))


(defn current-step [timer elapsed]
  (if (> elapsed 0)
    (->> timer
         :steps
         (filter #(> elapsed (min->ms (step-start-time timer %))))
         last)
    (first (:steps timer))))


(comment
  (step-index test-timer (current-step test-timer (min->ms 10))))


(defn time->angle [timer min]
  (* 360 (/ min (full-duration timer))))


(defn step->angle [timer step]
  (let [duration (step-end-time timer step)]
    (time->angle timer duration)))


(comment
  (step->angle test-timer test-step))


(defn format-time 
  "See this article:
   https://tech.toryanderson.com/2020/10/22/zero-padding-and-truncating-with-string-formats-in-clojurescript/"
  [ms]
  (let [minutes (js/Math.floor (/ ms 1000 60))
        seconds (js/Math.floor (mod (/ ms 1000) 60))]
    (gstring/format "%02d:%02d" minutes seconds)))


(comment
  (format-time 1000)
  (gstring/format "%02d" 1))


(defn play-state [data]
  (cond
    (:started-at data) :running
    (pos? (:previous-time data)) :paused
    :else :stopped))

(comment
  (play-state initial-state))