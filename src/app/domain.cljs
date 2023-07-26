(ns app.domain)


(defn timer [name]
  {:id (random-uuid)
   :name name
   :steps [{:id (random-uuid)
            :duration 18
            :timeout nil}
           {:id (random-uuid)
            :duration 5
            :timeout nil}]})


(defn initial-state []
  {:page :index
   :current-timer nil
   :modal nil
   :debug? false
   :timers (for [i (range 3)]
             (timer (str "Timer " (inc i))))})


(defn min->ms [min]
  (* 1000 60 min))


(defn duration-min [timer]
  (->> (:steps timer)
       (map :duration)
       (reduce +)))


(defn by-id [coll id]
  (first
   (filter #(= id (:id %)) coll)))


(defn duration-inclusive [timer step]
  "Returns the duration of the timer up to and including the given step."
  (let [index (.indexOf (:steps timer) step)
        steps-until-and-including (take (inc index) (:steps timer))]
    (->> steps-until-and-including
         (map :duration)
         (reduce +))))


(defn division->degrees [num den]
  (* 360 (/ num den)))


(defn duration-inclusive->degrees [timer step]
  (let [duration-inclusive (duration-inclusive timer step)
        duration-min (duration-min timer)]
    (division->degrees duration-inclusive duration-min)))


(defn progress->degrees [timer progress]
  (let [duration-min (duration-min timer)]
    (division->degrees progress duration-min)))


(def test-timer (first (:timers initial-state)))


(def test-step (second (:steps test-timer)))


(comment
  (duration-min test-timer)
  (duration-inclusive test-timer test-step)
  (division->degrees (duration-inclusive test-timer test-step) (duration-min test-timer))
  (initial-state)
  (new-timer "Test")
  )