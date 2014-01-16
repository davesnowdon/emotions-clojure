(ns emotions.core
  (:require [emotions.util :refer :all]))

;; motivations are maps

;; satisfaction vectors are maps with the motivation name as a key

;; percepts are maps with a key :satisfaction-vector

;; define default range for motivation desire values
(def default-min-desire 0.0)
(def default-max-desire 1.0)
(def default-max-change 1.0)
(def default-max-change-delta 0.01)

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
  (if-let [percept-score ((:id motivation) (:satisfaction-vector percept))]
    (assoc motivation :desire (+ (:desire motivation) percept-score))
    motivation))

(defn add-percepts
  "Modify a sequence of motivations by adding the values from a sequence of percepts"
  [motivations percepts]
  (map #(reduce add-percept % percepts) motivations))

(defn limit-desire-to-range
  "Limit the value of a motivations desire to that defined by its range"
  [motivation]
  (let [desire (:desire motivation)
        min-desire (:min-desire motivation default-min-desire)
        max-desire (:max-desire motivation default-max-desire)]
    (cond
     (< desire min-desire) (assoc motivation :desire min-desire)
     (> desire max-desire) (assoc motivation :desire max-desire)
     :else motivation)))

(defn limit-desire-change
  "Restrict change in desire to be within :max-change"
  [motivation]
  (let [desire (:desire motivation)
        last-desire (:last-desire motivation)
        max-change (:max-change motivation default-max-change)
        max-change-delta (:max-change-delta motivation default-max-change-delta)
        change (- desire last-desire)]
    (if (> (Math/abs change) max-change)
      (if (neg? change)
        (assoc motivation :desire (- last-desire max-change)
                          :max-change (+ max-change max-change-delta))
        (assoc motivation :desire (+ last-desire max-change)
                          :max-change (+ max-change max-change-delta)))
      motivation)))

(defn start-update
  "Save the current state of all motivations when starting a new update cycle"
  [motivations]
  (map #(assoc % :last-desire (:desire %)) motivations))

(defn apply-update-limits
  "Apply update limits to a motivation"
  [motivation]
  (-> motivation
      limit-desire-change
      limit-desire-to-range))

(defn end-update
  "Apply limits at end of an update cycle"
  [motivations]
  (map apply-update-limits motivations))

(defn update-motivations
  "Update the motivations desire values for a time step given a sequence of percepts"
  [motivations percepts]
  (-> motivations
      (start-update)
      (decay-all-motivations)
      (add-percepts percepts)
      (end-update)))

(defn motivations->sv
  "Create a satisfaction vector from a sequence of motivations with each key in the satisfaction vector being the motivation name and the value being the associated desire score"
  [motivations]
  (->> motivations
       (map (juxt :id :desire))
       (map (partial apply hash-map))
       (apply merge-with concat)))

(defn motivations->layer-scores
  "Return the normalised desire scores for each layer"
  [motivations]
  (let [layer-totals (->> motivations
                          (map (juxt :layer :desire))
                          (map (partial apply hash-map))
                          (apply merge-with +))
        layer-counts (frequencies (map :layer motivations))]
    (merge-with / layer-totals layer-counts)))
