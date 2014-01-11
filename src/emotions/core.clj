(ns emotions.core)

;; motivations are maps

;; satisfaction vectors are maps with the motivation name as a key

;; percepts are maps with a key :satisfaction-vector

(defn decay-motivation
  "Decay a motivation's current desire by the amount of its decay rate"
  [motivation]
  (assoc motivation :desire (+ (:desire motivation) (:decay-rate motivation))))

(defn decay-all-motivations
  "Decay a sequence of motivations"
  [motivations]
  (map decay-motivation motivations))

(defn add-percept
  "Add the from a percept's satisfaction vector with the motivations current desire"
  [motivation percept]
  (if-let [percept-score ((:name motivation) (:satisfaction-vector percept))]
    (assoc motivation :desire (+ (:desire motivation) percept-score))
    motivation))
