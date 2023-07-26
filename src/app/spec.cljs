(ns app.spec
  (:require
   [cljs.spec.alpha :as s]
   [cljs.spec.test.alpha :as stest]
   [clojure.test.check.generators :as gen]))


(defn random-string-gen [size]
  (gen/fmap
   #(apply str %)
   (gen/vector gen/char-alphanumeric size)))

(s/def ::id (s/with-gen
              (s/and string? (complement empty?))
              #(random-string-gen 36)))

(s/def ::name (s/with-gen
                string?
                #(random-string-gen 10)))

(s/def ::duration (s/and int? pos?))

(s/def ::elapsed-time (s/and number? pos?))

(s/def ::playback-state #{:playing :paused :stopped})

(s/def ::timeout (s/nilable (s/and int? pos?)))

(s/def ::step (s/keys :req-un [::id ::duration ::timeout]))

(s/def ::steps (s/coll-of ::step))

(s/def ::timer (s/keys :req-un [::id ::elapsed-time  ::name ::steps]))

(s/def ::current-timer (s/nilable ::id))

(s/def ::timers (s/coll-of ::timer :min-count 0))

(s/def ::page #{:index :edit :run})

(defn valid-current-timer? [state]
  (let [timer-ids (map ::id (::timers state))
        current-timer (::current-timer state)]
    (or (nil? current-timer)
        (some #(= % current-timer) timer-ids))))

(s/def ::state
  (s/and
   (s/keys :req-un [::playback-state ::current-timer ::page ::timers])
   valid-current-timer?))

(comment
  (gen/sample (s/gen ::state) 1)
  )