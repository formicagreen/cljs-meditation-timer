(ns app.domain
  (:require 
   [goog.string :as gstring]))


(defn step [duration]
  {:id (random-uuid)
   :duration duration})


(defn timer [name]
  {:id (random-uuid)
   :name name
   :steps (vec (for [i (range 3)] ; we need a vector here otherwise we will get a LazySeq which can't be accessed by index
                 (step 5)))})


(defn initial-state []
  {:page :index
   :current-timer nil
   :modal nil
   :started-at nil
   :elapsed 0
   :previous-time nil
   :debug? false
   :speedup 1000
   :timeouts {}
   :animation-frame nil
   :timers (vec (for [i (range 3)] ; we need a vector here otherwise we will get a LazySeq which can't be accessed by index
                  (timer (str "Timer " (inc i)))))})


(def test-data (initial-state))


(def test-timer (first (:timers test-data)))


(def test-step (first (:steps test-timer)))


(defn min->ms [min]
  (* 1000 60 min))


(defn ms->min [ms]
  (/ ms (* 1000 60)))


(defn duration [timer]
  (->> (:steps timer)
       (map :duration)
       (reduce +)))


(defn where [coll key val]
  (->> coll
       (filter #(= (get % key) val))
       first))


(defn index-where
  [coll key val]
  (->> coll
       (map key)
       (keep-indexed #(when (= %2 val) %1))
       first))


(defn path 
  ([data timer]
            (let [timer-idx (.indexOf (:timers data) timer)]
              [:timers timer-idx]))
  ([data timer step]
   (let [timer-idx (.indexOf (:timers data) timer)
         step-idx  (.indexOf (get-in data [:timers timer-idx :steps]) step)]
     [:timers timer-idx :steps step-idx])))


(comment 
  (path test-data test-timer)
  (path test-data test-timer test-step)
)


(defn clamp [n vmin vmax]
  (max vmin (min vmax n)))


(defn update-duration 
  [data timer step duration]
  (assoc-in data (path data timer step) (clamp duration 1 999)))


(defn rename-timer [data timer name]
  (let [timer-idx (.indexOf (:timers data) timer)
        path [:timers timer-idx :name]]
    (assoc-in data path name)))


(defn index-of [coll item]
  (->>
   coll
   (keep-indexed #(when (= item %2) %1))
   first))


(defn remove-where [coll val]
  (->> coll
       (remove #(= val %))))


(defn delete-step [data timer step]
  (let [timer-idx (.indexOf (:timers data) timer)
        path [:timers timer-idx :steps]]
    (update-in data (path data timer step) remove-where step)))


(comment 
  (delete-step test-data test-timer test-step)
  )


(defn delete-timer [data timer]
  (update data :timers
          (fn [timers] (remove #(= timer %) timers))))


(comment
  (delete-timer test-data test-timer)
  )


(defn add-step [data timer]
  (let [timer-idx (.indexOf (:timers data) timer)
        path [:timers timer-idx :steps]]
    (update-in data path conj (step 5))))


(comment
  (index-where [{:id 1} {:id 2}] :id 2)
  (assoc-in [[1 2 3] 2 3] [0 0] "a") 
  )


(defn duration-inclusive
  "Returns the duration of the timer up to and including the given step."
  [timer step]
  (let [index (.indexOf (:steps timer) step)
        steps-until-and-including (take (inc index) (:steps timer))]
    (->> steps-until-and-including
         (map :duration)
         (reduce +))))


(defn min->angle [timer ms]
  (* 360 (/ (ms->min ms) (duration timer))))


(defn step->angle [timer step]
  (let [duration (duration-inclusive timer step)]
    (min->angle timer duration)))


(defn format-time [ms]
  (let [minutes (js/Math.floor (/ ms 1000 60))
        seconds (js/Math.floor (mod (/ ms 1000) 60))]
    (str (gstring/format "%02d" minutes) ":" (gstring/format "%02d" seconds))))


(comment
  (format-time 1000)
  )


(defn play-state [data]
  (cond
    (:started-at data) :running
    (:previous-time data) :paused
    :else :stopped))

(comment
  (play-state test-data)
  )