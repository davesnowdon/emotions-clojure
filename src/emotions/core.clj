(ns emotions.core
  (:require [emotions.util :refer :all]))

;; motivations are maps

;; satisfaction vectors are maps with the motivation name as a key

;; percepts are maps with a key :satisfaction-vector

;; define default range for motivation desire values
(def default-min-desire 0.0)
(def default-max-desire 1.0)

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

(defn add-percepts
  "Modify a sequence of motivations by adding the values from a sequence of percepts"
  [motivations percepts]
  (map #(reduce add-percept % percepts) motivations))

(defn min-desire
  "Get the minimum desire from motivation or use default"
  [motivation]
  (if-let [min-desire (:min-desire motivation)]
    min-desire
    default-min-desire))

(defn max-desire
  "Get the maximum desire from motivation or use default"
  [motivation]
  (if-let [max-desire (:max-desire motivation)]
    max-desire
    default-max-desire))

(defn limit-desire-to-range
  "Limit the value of a motivations desire to that defined by its range"
  [motivation]
  (let [desire (:desire motivation)
        min-desire (min-desire motivation)
        max-desire (max-desire motivation)]
    (cond
     (< desire min-desire) (assoc motivation :desire min-desire)
     (> desire max-desire) (assoc motivation :desire max-desire)
     :else motivation)))

(defn limit-desire-change
  "Restrict change in desire to be within :max-change"
  [motivation]
  (let [desire (:desire motivation)
        last-desire (:last-desire motivation)
        max-change (:max-change motivation)
        change (- desire last-desire)]
    (if (> (Math/abs change) max-change)
      (if (neg? change)
        (assoc motivation :desire (- last-desire max-change))
        (assoc motivation :desire (+ last-desire max-change)))
      motivation)))

(defn start-update
  "Save the current state of all motivations when starting a new update cycle"
  [motivations]
  (map #(assoc % :last-desire (:desire %)) motivations))
