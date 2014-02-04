(ns emotions.core
  (:require [emotions.util :refer :all]))

;; motivations are maps

;; satisfaction vectors are maps with the motivation name as a key

;; percepts are maps with a key :satisfaction-vector

;; control points map motivation threshold scores (called an
;; expression vector) to points in valence/arousal space
;; control point {:valence 1.0 :arousal 1.0 :expression-vector {} }

;; define default range for motivation desire values
(def default-min-desire 0.0)
(def default-max-desire 1.0)
(def default-max-change 1.0)
(def default-max-change-delta 0.01)
;; how much a motivation has to be inhibited by in order to have
;; its max-change reduced
(def default-max-change-threshold 0.1)

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
  (if-let [percept-score ((:id motivation) (:satisfaction-vector percept) 0.0)]
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
  "Restrict change in desire to be within :max-delta"
  [motivation]
  (let [desire (:desire motivation)
        last-desire (:last-desire motivation)
        max-change (:max-delta motivation default-max-change)
        max-change-delta (:max-delta-delta motivation default-max-change-delta)
        change (- desire last-desire)]
    (if (> (Math/abs change) max-change)
      (if (neg? change)
        (assoc motivation :desire (- last-desire max-change)
                          :max-delta (+ max-change max-change-delta))
        (assoc motivation :desire (+ last-desire max-change)
                          :max-delta (+ max-change max-change-delta)))
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

(defn motivations->layers
  "Given a sequence of motivations return a map from motivation id to layer id"
  [motivations]
  (letfn [(add-layer [a m] (assoc a (:id m) (:layer m)))]
    (reduce add-layer {} motivations)))

(defn scale-layer-scores
  "Takes an ordered list of layers from most inhibitory to least and a map of layers to normalised motivation and a scaling factor. Returns a map with the scaling factor to apply to each layer"
  [layers layer-scores layer-multipliers]
  (loop [remaining layers
         scale 1.0
         accum {}]
    (if (seq remaining)
      (let [layer (first remaining)
            multiplier (layer-multipliers layer 1.0)
            score (layer-scores layer 0.0)
            new-scale (bounded- scale (* score multiplier) 0.0)]
        (recur (rest remaining) new-scale (assoc accum layer scale)))
      accum)))

(defn inhibit
  "Given an ordered sequence of layers, layer scores, a map from motivation to layer and a satisfaction vector return a satisfaction vector with the values modified according to the inhibiting factor of each layer"
  [layers layer-scores motivations2layers satisfaction-vector layer-multipliers]
  (let [scales (scale-layer-scores layers layer-scores layer-multipliers)]
;;    (println "Layer scores" layer-scores)
;;    (println "Scaled Layer scores" scales)
    (letfn [(new-desire [k v] (* v (scales (motivations2layers k))))]
      (reduce (fn [r [k v]] (assoc r k (new-desire k v))) {}
              satisfaction-vector))))

(defn adjust-max-deltas
  "Reduce the max delta of all motivations where the final score after inhibition is less than the raw desire value from the motivation"
  [motivations inhibited-sv adjustment]
  (letfn [(adjust-m [m]
            (let [id (:id m)
                  desire (:desire m)
                  max-delta (:max-delta m)
                  i-desire (inhibited-sv id)]
              (if (> (Math/abs (- desire i-desire))
                     default-max-change-threshold)
                (assoc m :max-delta
                       (bounded- max-delta adjustment adjustment))
                m)))]
    (map adjust-m motivations)))

(defn percepts->motivations+sv
  "Given a sequence of percepts update the motivations and generate the corresponding satisfaction vector. Returns a vector containing the new motivations sequence as the first element and the satisfaction vector as the second element"
  [layers layer-multipliers motivations percepts]
  (let [pm (update-motivations motivations percepts)
        sv (motivations->sv pm)
        ls (motivations->layer-scores pm)
        m2l (motivations->layers pm)
        isv1 (inhibit (reverse layers) ls m2l sv layer-multipliers)
        isv2 (inhibit layers ls m2l isv1 layer-multipliers)
        pma (adjust-max-deltas pm isv2 default-max-change-delta)]
    [pma isv2]))

(defn expression-vector-distance
  "Returns value from 0.0 to 1.0 representing the similarity between an expression vector and a satisfaction vector. A value of 0.0 indicates a perfect match"
  [ev sv]
  (if (> (count ev) 0)
    (letfn [(sv-diff [a [k v]] (+ a  (if (nil? (sv k)) 1.0 (Math/abs (- v (sv k))))))]
      (/ (reduce sv-diff 0.0 ev) (count ev)))
    1.0))

(defn sv->valence+arousal
  "Calculate the valence and arousal scores for a given satisfaction vector"
  [control-points sv]
  (letfn [(add-dist [cp]
            (let [dist (expression-vector-distance (:expression-vector cp) sv)]
              (assoc cp :distance dist :weight (- 1.0 dist))))
          (ord-val [k acc cp] (+ acc (* (:weight cp) (cp k))))]
    (let [with-dist (map add-dist control-points)
          valence (reduce (partial ord-val :valence) 0.0 with-dist)
          arousal (reduce (partial ord-val :arousal) 0.0 with-dist)
          total-weight (reduce (fn [acc cp] (+ acc (:weight cp))) 0.0 with-dist)]
      {:valence (/ valence total-weight) :arousal (/ arousal total-weight)})))
