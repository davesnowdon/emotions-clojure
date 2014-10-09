(ns emotions.core
  (:require [emotions.util :refer :all]
            [emotions.serialise :refer :all]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.edn :as edn]))

;; motivations are maps, with keys
;; learning-window-ms - learning window in milliseconds

;; satisfaction vectors are maps with the motivation name as a key

;; percepts are maps with keys
;; :satisfaction-vector - emotional state associated with this percept
;; :learning-vector - emotional impact of percept
;; :satisfaction-vector-obs - global satisfaction vector when percept
;;     observed
;; :timestamp - when the percept occured
;; :other-agents - other agents or people associated with the event (set)
;; :locations - indications of where the percept took place (set)
;; :stm-entry - time added to short-term memory
;; :stm-expiration - when does this percept get removed from short-term
;;     memory
;; :ltm-entry - time added to long-term memory
;; :ltm-update-count - number of times record has been updated
;; :data - percept, location or agent specific data is in a map under
;;     this key

;; each motivation has zero or more attractors which are used to map
;; from a satisfaction vector to valence/arousal space

;; define default range for motivation desire values
(def default-min-desire 0.0)
(def default-max-desire 1.0)
(def default-max-change 1.0)
(def default-max-change-delta 0.01)
;; how much a motivation has to be inhibited by in order to have
;; its max-change reduced
(def default-max-change-threshold 0.1)

;; long-term memory lookup weights
(def ltm-name-weight 1/3)
(def ltm-agents-weight 1/3)
(def ltm-location-weight 1/3)
(def ltm-percept-max-age-seconds (* 365 24 60 60))

;; the min average value of a learning vector for a percept in order
;; to make it worth remembering
(def ltm-default-learning-vector-threshold 0.1)
;; magnitude of a single learning vector element that makes it worth
;; storing in long-term memory
(def ltm-default-learning-vector-element-threshold 0.3)

(defn default-decay-motivation
  "Decay a motivation's current desire by the amount of its decay rate in seconds according to the time elapsed since the last update"
  [motivation time-since-update]
  (let [delta (* (:decay-rate motivation) time-since-update)]
    (assoc motivation :desire (+ (:desire motivation) delta))))

(defn decay-all-motivations
  "Decay a sequence of motivations according to the time since the last update"
  [motivations time-since-update]
  (letfn [(do-decay [m tsu]
            ((:decay-fn m default-decay-motivation) m tsu))]
    (map #(do-decay % time-since-update) motivations)))

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
  [motivations percepts time-since-update]
  (-> motivations
      (start-update)
      (decay-all-motivations time-since-update)
      (add-percepts percepts)
      (end-update)))

(defn make-motivation-map
  "Make a map from one key in the sequence of motivations to another"
  [motivations from-key to-key]
  (->> motivations
       (map (juxt from-key to-key))
       (map (partial apply hash-map))
       (apply merge-with concat)))

(defn motivations->sv
  "Create a satisfaction vector from a sequence of motivations with each key in the satisfaction vector being the motivation name and the value being the associated desire score"
  [motivations]
  (make-motivation-map motivations :id :desire))

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
  [layers layer-multipliers motivations percepts time-since-update]
  (let [pm (update-motivations motivations percepts time-since-update)
        sv (motivations->sv pm)
        ls (motivations->layer-scores pm)
        m2l (motivations->layers pm)
        isv1 (inhibit (reverse layers) ls m2l sv layer-multipliers)
        isv2 (inhibit layers ls m2l isv1 layer-multipliers)
        pma (adjust-max-deltas pm isv2 default-max-change-delta)]
    [pma isv2]))

;; code to compute valence & arousal from satisfaction vector
(defn proportional-attractor
  "Return an attractor who's weight varies proportionally with a desire value"
  [valence, arousal, scale]
  (fn [desire] {:valence valence :arousal arousal :weight (* desire scale)}))

(defn inverse-attractor
  "Return an attractor who's weight varies inversely with a desire value"
  [valence, arousal, scale]
  (fn [desire] {:valence valence :arousal arousal :weight (* (- 1.0 desire) scale)}))

(defn sum-attractors
  "Sum 2 attractors"
  [a b]
  {:valence (interpolate-unnormalised (:valence a) (:weight a)
                                      (:valence b) (:weight b))
   :arousal (interpolate-unnormalised (:arousal a) (:weight a)
                                      (:arousal b) (:weight b))
   :weight (+ (:weight a) (:weight b))})

(defn combine-attractors
  "Take a sequence of attractors and produce an overall valence and arousal"
  [attractors]
  (let [sum (reduce sum-attractors attractors)]
    {:valence (/ (:valence sum) (:weight sum))
     :arousal (/ (:arousal sum) (:weight sum))
     :weight 1.0}))

(defn attractor-fn->attractor
  "Given a desire value get the attractors for a motivation"
  [desire fns]
  (map (fn [a] (a desire)) fns))

(defn motivations->attractor-fns
  "Make a map from motivation id to sequence of attractors"
  [motivations]
  (make-motivation-map motivations :id :attractors))

(defn get-attractors
  "Take a satisfaction vector and motivations and compute the set of all attractors"
  [motivations sv]
  (let [attractor-fns (motivations->attractor-fns motivations)]
    (map (fn [[k v]] (attractor-fn->attractor v (k attractor-fns))) sv)))

(defn sv->valence+arousal
  "Calculate the valence and arousal scores for a given satisfaction vector"
  [motivations sv]
  (combine-attractors (get-attractors motivations sv)))

;;
;; Functions related to short-term memory
;;

(defn equivalent-percepts
  "Return true if two percepts are equivalent"
  [p1 p2]
  (and (= (:name p1) (:name p2))
       (= (:other-agents p1) (:other-agents p2))
       (= (:locations p1) (:locations p2))))

(defn- make-constant-map
  "Create a map with the specified set of keys with all values being the specified constant"
  [ks value]
  (apply merge (map #(assoc {} % value) ks)))

(defn- make-lv
  "Create a learning vector given a sequence of keys to use"
  [svk]
  (make-constant-map svk 0.0))

(defn- add-stm-keys
  ([percepts sv retain-period]
     (add-stm-keys percepts sv retain-period (t/now)))

  ([percepts sv retain-period timestamp]
     (let [exp (t/plus timestamp retain-period)
           lv (make-lv (keys sv))
           to-merge {:stm-entry timestamp
                     :stm-expiration exp
                     :stm-last-update timestamp
                     :learning-vector lv
                     :satisfaction-vector-obs sv}]
       (map #(merge % to-merge) percepts))))

(defn- stm-exists
  [stm percept equiv-fn]
  (some #(equiv-fn % percept) stm))

(defn- stm-equivalent-percepts
  [stm new-percepts equiv-fn]
  (filter #(stm-exists stm % equiv-fn) new-percepts))

(defn- stm-new-percepts
  [stm new-percepts equiv-fn]
  (filter #((complement stm-exists) stm % equiv-fn) new-percepts))

(defn- stm-extend-equiv
  [stm percepts equiv-fn]
  (if (empty? percepts)
    stm
    (->> (for [s stm p percepts]
           (if (equiv-fn s p)
             (assoc s :stm-expiration (:stm-expiration p))
             s))
         (filter identity))))

(defn short-term-memory-add
  "Add new percepts to short-term memory if they are not equivalent to existin percepts as defined by equiv-fn"
  ([stm percepts global-sv equiv-fn retain-period]
     (short-term-memory-add stm percepts global-sv equiv-fn retain-period (t/now)))

  ([stm percepts global-sv equiv-fn retain-period timestamp]
     (if (empty? percepts)
       stm
       (let [with-expire
             (add-stm-keys percepts global-sv retain-period timestamp)
             new-percepts
             (stm-new-percepts stm with-expire equiv-fn)
             equiv-percepts
             (stm-equivalent-percepts stm with-expire equiv-fn)]
         (concat (stm-extend-equiv stm equiv-percepts equiv-fn) new-percepts)))))

(defn short-term-memory-expired
  "Remove percepts that have reached their expiration time. Returns a map containing the new short-term memory as :stm and the expired percepts with key :expired"
  ([stm]
     (short-term-memory-expired stm (t/now)))

  ([stm timestamp]
     (loop [remaining stm expired #{} not-expired #{}]
       (if (not (seq remaining))
         (assoc {} :stm not-expired :expired expired)
         (let [p (first remaining)
               is-expired (t/after? timestamp (:stm-expiration p))
               new-exp (if is-expired (conj expired p) expired)
               new-not (if is-expired not-expired (conj not-expired p))]
           (recur (rest remaining) new-exp new-not))))))

(defn- lv-element-delta
  "Return the change for a given element of the learning vector"
  [sve-delta interval-ms lw-ms stm-time-ms]
  (if (> stm-time-ms lw-ms)
    0
    (* sve-delta (/ (float interval-ms) lw-ms))))

(defn- adjust-learning-vector
  [percept global-sv lws timestamp]
  (let [stm-time (millis-diff timestamp (:stm-entry percept))
        upd-interval (millis-diff timestamp (:stm-last-update percept))
        sv-obs (:satisfaction-vector-obs percept)
        sv-delta (merge-with - global-sv sv-obs)
        lv-delta (apply merge
                        (map (fn [k]
                               (hash-map k (lv-element-delta
                                            (sv-delta k)
                                            upd-interval
                                            (lws k)
                                            stm-time)))
                             (keys sv-delta)))
        new-lv (merge-with + (:learning-vector percept) lv-delta)]
    (assoc percept :learning-vector new-lv)))

(defn short-term-memory-learn
  "Takes the contents of short-term memory, the interval since the last update, the global satisfaction vector and the set of motivations and updates :learning-vector for each percept"
  ([stm global-sv motivations]
     (short-term-memory-learn stm global-sv motivations (t/now)))

  ([stm global-sv motivations timestamp]
     (let [lws (make-motivation-map motivations :id :learning-window)]
       (map #(adjust-learning-vector % global-sv lws timestamp) stm))))

;;
;; Functions related to long-term memory
;;
(defn percept->lv
  [percept]
  (:learning-vector percept))

(defn- percept->lv-values
  [percept]
  (vals (percept->lv percept)))

(defn percept-lv-mean
  [percept]
  (mean (abs-all (percept->lv-values percept))))

(defn percept-lv-max
  [percept]
  (apply max (abs-all (percept->lv-values percept))))

(defn percept-significant?
  "Return true if a percept is emotionally significant (worth storing in long-term memory)"
  ([percept]
     (percept-significant? percept
                           ltm-default-learning-vector-threshold
                           ltm-default-learning-vector-element-threshold))

  ([percept mean-threshold element-threshold]
     (or (> (percept-lv-mean percept) mean-threshold)
         (> (percept-lv-max percept) element-threshold))))

(defn- percept->ltm-key
  [percept]
  [(:name percept) (set (:other-agents percept)) (set (:locations percept))])

(defn long-term-memory-init
  []
  {:percepts {} :agents {} :locations {}})

(defn ltm-augment
  "Add the features required on an item when adding it to LTM"
  [item timestamp sv]
  (assoc item :ltm-entry timestamp
              :ltm-update-count 1
              :satisfaction-vector sv))

(defn long-term-memory-find-agent
  "Retrieve an agent from long-term memory using its id"
  [ltm id]
  (get-in ltm [:agents id]))

;; TODO detect existing agents by content as well as ID
(defn long-term-memory-add-agent
  "Add an agent to long-term memory by id"
  ([ltm agent sv]
     (long-term-memory-add-agent ltm agent sv (t/now)))
  ([ltm agent sv timestamp]
     (let [existing (long-term-memory-find-agent ltm (:id agent))]
       (if existing
         ltm
         (assoc-in ltm [:agents (:id agent)]
                   (ltm-augment agent timestamp sv))))))

(defn long-term-memory-find-location
  "Retrieve a location by id"
  [ltm id]
  (get-in ltm [:locations id]))

;; TODO detect existing locations by content as well as ID
(defn long-term-memory-add-location
  "Store a location by id in long-term memory"
  ([ltm location sv]
     (long-term-memory-add-location ltm location sv (t/now)))
  ([ltm location sv timestamp]
     (let [existing (long-term-memory-find-location ltm (:id location))]
       (if existing
         ltm
         (assoc-in ltm [:locations (:id location)]
                   (ltm-augment location timestamp sv))))))

;; TODO need to handle new items that don't already have an ID
(defn- ltm-add-items
  "Takes ltm a list of items and uses the supplied function to add them to LTM returning the new LTM and a list of ids"
  [ltm items add-fn]
  (let [onlyids (filter (complement map?) items)
        njids (filter map? items)
        newltm (reduce add-fn ltm njids)
        allids (concat onlyids (map :id njids))]
    {:ltm newltm :ids allids}))

(defn- ltm-percept-add
  "Add a new percept to LTM"
  [ltm percept timestamp]
  (assoc-in ltm [:percepts (percept->ltm-key percept)]
                 (assoc percept :ltm-entry timestamp
                                :ltm-update-count 1)))

(defn sv+lv->sv
  "Update a satisfaction vector using a learning vector the relative age and the number of times the sv has been updated"
  [sv lv num-updates sv-age-seconds]
  (let [unit-weight (/ 1 (+ num-updates 1))
        sf (bounded- ltm-percept-max-age-seconds sv-age-seconds 0)
        nsf (/ (float sf) ltm-percept-max-age-seconds)
        sv-weight (/ (+ (* unit-weight num-updates) nsf) 2.0)
        lv-weight (- 1.0 sv-weight)]
    (merge-with +
                (merge-with * sv (make-constant-map (keys sv) sv-weight))
                (merge-with * lv (make-constant-map (keys sv) lv-weight)))))

(defn- ltm-percept-update
  "Update a percept already in LTM"
  [ltm percept timestamp]
  (let [key (percept->ltm-key percept)
        existing (get-in ltm [:percepts key])
        age (seconds-diff timestamp (:ltm-entry existing))
        num-updates (:ltm-update-count existing)
        sv (:satisfaction-vector existing)
        lv (:learning-vector percept)
        adjusted-sv (sv+lv->sv sv lv num-updates age)]
    (assoc-in ltm [:percepts key]
              (assoc existing :satisfaction-vector adjusted-sv
                     :ltm-update-count (+ num-updates 1)))))

;; TODO need to handle new items that don't already have an ID
(defn long-term-memory-add-percept
  "Add a percept to long-term memory, adding new locations and agents as necessary"
  ([ltm percept]
     (long-term-memory-add-percept ltm percept (t/now)))

  ([ltm percept timestamp]
     (let [old-sv (:satisfaction-vector percept)
           lv (:learning-vector percept)
           sv (merge-with + old-sv lv)
           {altm :ltm aids :ids}
           (ltm-add-items ltm (:other-agents percept)
                          #(long-term-memory-add-agent %1 %2 sv timestamp))
           {lltm :ltm lids :ids}
           (ltm-add-items altm (:locations percept)
                          #(long-term-memory-add-location %1 %2 sv timestamp))
           npercept (assoc percept :locations (set lids)
                           :other-agents (set aids))
           key (percept->ltm-key npercept)
           existing (get-in lltm [:percepts key])]
       (if existing
         (ltm-percept-update lltm npercept timestamp)
         (ltm-percept-add lltm npercept timestamp)))))

(defn- ltm-name-score
  [desired-name percept]
  (if (= desired-name (:name percept))
    ltm-name-weight 0.0))

(defn- weighted-proportion-in-set
  "Return the proportion of items in the desired set that occur in the actual set multiplied by the supplied weight"
  [desired actual weight]
  (* (/ (count (filter desired actual))
        (count desired))
     weight))

(defn- ltm-location-score
  [desired-locations percept]
  (weighted-proportion-in-set desired-locations
                              (:locations percept)
                              ltm-location-weight))

(defn- ltm-agents-score
  [desired-agents percept]
  (weighted-proportion-in-set desired-agents
                              (:other-agents percept)
                              ltm-agents-weight))

(defn- ltm-percept-score
  [name locations agents percept]
  (+ (ltm-name-score name percept)
     (ltm-location-score locations percept)
     (ltm-agents-score agents percept)))

(defn- ltm-highest-scoring-percept
  "Return the percept in long-term memory that best matches the supplied percept"
  [ltm percept]
  (let [key (percept->ltm-key percept)
        name (:name percept)
        locations (set  (:locations percept))
        agents (set (:other-agents percept))
        exact-match (get-in ltm [:percepts key])]
    (if exact-match
      [1.0 exact-match]
      (->> (vals (:percepts ltm))
           (map (fn [p] [(ltm-percept-score name locations agents p) p]))
           (sort-by #(% 0) #(compare %2 %1))
           first))))

(defn long-term-memory-get-sv
  "Return satisfaction vector and weight that best matches supplied percept"
  [ltm percept]
  (let [hsp (ltm-highest-scoring-percept ltm percept)]
    (if hsp
      {:satisfaction-vector (:satisfaction-vector (hsp 1))
       :weight (hsp 0)})))

(defn long-term-memory-read
  "Read from a file into a long-term memory structure"
  [filename]
  (with-open [r (java.io.PushbackReader. (clojure.java.io/reader filename))]
    (binding [*read-eval* false]
      (deserialise (edn/read r)))))

(defn long-term-memory-read-string
  "Read from a string into a long-term memory structure"
  [s]
  (deserialise (edn/read-string s)))


(defn long-term-memory-write
  "Write the contents of long-term memory to file"
  [ltm filename]
  (with-open [w (clojure.java.io/writer filename)]
    (binding [*out* w]
      (pr (serialise ltm)))))

(defn long-term-memory-write-string
  "Write the contents of long-term memory as a string"
  [ltm]
  (pr-str (serialise ltm)))
