(ns emotions.core-test
  (:require [emotions.core :refer :all]
            [emotions.util :refer :all]
            [expectations :refer :all]))

(def hunger {:name :hunger, :desire 0.0, :decay-rate 0.1, :max-change 0.3})

(def percept {:satisfaction-vector {:hunger 0.5, :survival 0.0}})

;; desire should increase by value of :decay-rate
(expect 0.1 (:desire (decay-motivation {:desire 0.0, :decay-rate 0.1})))

;; decay all motivations should decay each motivation in a sequence
(expect [{:name :hunger, :desire 0.1, :decay-rate 0.1, :max-change 0.3}]
        (decay-all-motivations [hunger]))

;; adding a percept should modify the motivations desire by the value in the
;; percept's satisfaction vector the corresponds to the motivation
(expect 0.5 (:desire (add-percept hunger percept)))

;; add-percepts should add all percepts to all motivations
(expect [{:name :hunger, :desire 0.5, :decay-rate 0.1, :max-change 0.3}]
        (add-percepts [hunger] [percept]))

;; check that we save the value of desire when starting an update
(expect 0.5 (:last-desire (first (start-update [{:desire 0.5}]))))

;; desire should not change if it has changed less than :max-change
(expect 0.5 (:desire (limit-desire-change {:desire 0.5
                                           :last-desire 0.4
                                           :max-change 0.3})))

;; desire should be limited to not increasing by more than :max-change
(expect #(float= 0.7 %)
        (:desire (limit-desire-change {:desire 0.9
                                       :last-desire 0.4
                                       :max-change 0.3})))

;; desire should be limited to not decreasing by more than max change
(expect #(float= 0.1 %)
        (:desire (limit-desire-change {:desire 0.0
                                       :last-desire 0.4
                                       :max-change 0.3})))
