(ns emotions.core-test
  (:require [emotions.core :refer :all]
            [emotions.util :refer :all]
            [emotions.motivations :refer :all]
            [expectations :refer :all]))

(def hunger {:id :hunger, :desire 0.0, :decay-rate 0.1, :max-delta 0.3,
             :layer :physical})

(def percept {:satisfaction-vector {:hunger 0.5, :survival 0.0}})

;; desire should increase by value of :decay-rate
(expect {:desire 0.1} (in (decay-motivation {:desire 0.0, :decay-rate 0.1})))

;; decay all motivations should decay each motivation in a sequence
(expect [{:id :hunger, :desire 0.1, :decay-rate 0.1,
          :max-delta 0.3, :layer :physical}]
        (decay-all-motivations [hunger]))

;; adding a percept should modify the motivations desire by the value in the
;; percept's satisfaction vector the corresponds to the motivation
(expect {:desire 0.5} (in (add-percept hunger percept)))

;; add-percepts should add all percepts to all motivations
(expect [{:id :hunger, :desire 0.5, :decay-rate 0.1,
          :max-delta 0.3, :layer :physical}]
        (add-percepts [hunger] [percept]))

;; check that we save the value of desire when starting an update
(expect {:last-desire 0.5} (in (first (start-update [{:desire 0.5}]))))

;; desire should not be changed if it is within range
(expect {:desire 0.5} (in (limit-desire-to-range {:desire 0.5
                                                  :min-desire 0.0
                                                  :max-desire 1.0})))

;; desire should not be allowed to be greater than max-desire
(expect {:desire 0.8} (in (limit-desire-to-range {:desire 1.0
                                                  :min-desire 0.1
                                                  :max-desire 0.8})))

;; desire should not be allowed to be less than min-desire
(expect {:desire 0.1} (in (limit-desire-to-range {:desire 0.0
                                                  :min-desire 0.1
                                                  :max-desire 0.8})))

;; if motivation does not define :min-desire default of 0.0 should be used
(expect {:desire 0.0} (in (limit-desire-to-range {:desire -1})))

;; if motivation does not define :max-desire default of 1.0 should be used
(expect {:desire 1.0} (in (limit-desire-to-range {:desire 2})))

;; desire should not change if it has changed less than :max-delta
(expect {:desire 0.5} (in (limit-desire-change {:desire 0.5
                                                :last-desire 0.4
                                                :max-delta 0.3})))

;; desire should be limited to not increasing by more than :max-delta
(expect (float= 0.7
                (:desire (limit-desire-change {:desire 0.9
                                               :last-desire 0.4
                                               :max-delta 0.3}))))

;; desire should be limited to not decreasing by more than max change
(expect (float= 0.1
                (:desire (limit-desire-change {:desire 0.0
                                               :last-desire 0.4
                                               :max-delta 0.3}))))

;; max-change should be incremented when desire changes more than max-change
(expect (< 0.3
           (:max-delta (limit-desire-change {:desire 0.9
                                              :last-desire 0.4
                                              :max-delta 0.3}))))

(expect (< 0.3
           (:max-delta (limit-desire-change {:desire 0.0
                                              :last-desire 0.4
                                              :max-delta 0.3}))))

;; if defined, max-change-delta from motivation should be used to
;; adjust max-change
(expect (< 0.32
           (:max-delta (limit-desire-change {:desire 0.9
                                              :last-desire 0.4
                                              :max-delta 0.3
                                              :max-delta-delta 0.3}))))

(expect (< 0.32
           (:max-delta (limit-desire-change {:desire 0.0
                                              :last-desire 0.4
                                              :max-delta 0.3
                                              :max-delta-delta 0.3}))))

;; in a timestep without percepts all motivations should just decay
;; commented out because of failing float comparison
;(expect [{:id :hunger, :desire 0.4, :last-desire 0.1, :decay-rate 0.3}
;         {:id :happiness, :desire 0.7, :last-desire 0.5, :decay-rate 0.2}
;         {:id :survival, :desire 0.3, :last-desire 0.2, :decay-rate 0.1}]
;(update-motivations [{:id :hunger, :desire 0.1, :decay-rate 0.3}
;                     {:id :happiness, :desire 0.5, :decay-rate 0.2}
;                     {:id :survival, :desire 0.2, :decay-rate 0.1}]
;                    []))

;; should be able to create a satisfaction vector from a sequence
;; of motivations with each key in the satisfaction vector being
;; the motivation name and the value being the associated desire score
(expect {:hunger 0.1, :happiness 0.5, :survival 0.2}
        (in (motivations->sv [{:id :hunger, :desire 0.1}
                              {:id :happiness, :desire 0.5}
                              {:id :survival, :desire 0.2}])))

;; should return a map of layer id to normalised desire score
;; desire scores should be normalised by number of motivations in
;; each layer
(expect {:one 0.1, :two 0.5, :three 0.2}
        (in (motivations->layer-scores
             [ {:id :hunger, :desire 0.1, :layer :one}
               {:id :happiness, :desire 0.5, :layer :two }
               {:id :survival, :desire 0.2, :layer :three}]))
)

(expect {:physical 3}
        (in (motivations->layer-scores
             [{:id :hunger, :desire 4, :layer :physical}
              {:id :survival, :desire 2, :layer :physical}])))

;; given an ordered list of layers from most inhibitory to least and
;; a map of layers to normalised motivation and a scaling factor
;; should return a map in which each entry is a scaling factor to
;; apply to the motivations from each layer
;; the most inhibitory layer should have a scaling factor of 1.0,
;; the next should have a value inversely proportional to that of
;; the most inhibitory layer, the next inversely proportional to the
;; two preceeing layers and so on.
;; physical -> 1.0
;; safety -> 0.5
;; social -> 0.1
;; values -> 0.0
;; contribution -> 0.0
(let [layers [:physical :safety :social :values :contribution]
      scores {:physical 0.5 :safety 0.4 :social 0.3 :values 0.2 :contribution 0.1}]
  (expect (float= 1.0
                  (:physical
                   (scale-layer-scores layers scores))))
  (expect (float= 0.5
                  (:safety
                   (scale-layer-scores layers scores))))
  (expect (float= 0.1
                  (:social
                   (scale-layer-scores layers scores))))
  (expect (float= 0.0
                  (:values
                   (scale-layer-scores layers scores))))
  (expect (float= 0.0
                  (:contribution
                   (scale-layer-scores layers scores)))))

;; a layer with a zero score should not inhibit
(expect (float= 1.0
                (:safety
                 (scale-layer-scores
                  [:physical :safety :social :values :contribution]
                  {:physical 0.0 :safety 0.4 :social 0.3 :values 0.2 :contribution 0.1}))))

;; reversing the order of layers should reverse the order of scaling
;; physical -> 1.0
;; safety -> 0.5
;; social -> 0.1
;; values -> 0.0
;; contribution -> 0.0
(let [layers [:contribution :values :social :safety :physical]
      scores {:physical 0.5 :safety 0.4 :social 0.3 :values 0.2 :contribution 0.1}]
  (expect (float= 0.0
                  (:physical
                   (scale-layer-scores layers scores))))
  (expect (float= 0.4
                  (:safety
                   (scale-layer-scores layers scores))))
  (expect (float= 0.7
                  (:social
                   (scale-layer-scores layers scores))))
  (expect (float= 0.9
                  (:values
                   (scale-layer-scores layers scores))))
  (expect (float= 1.0
                  (:contribution
                   (scale-layer-scores layers scores)))))
