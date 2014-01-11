(ns emotions.core-test
  (:require [emotions.core :refer :all]
            [emotions.util :refer :all]
            [expectations :refer :all]))

(def hunger {:name :hunger, :desire 0.0, :decay-rate 0.1, :max-change 0.3})

(def percept {:satisfaction-vector {:hunger 0.5, :survival 0.0}})

;; desire should increase by value of :decay-rate
(expect {:desire 0.1} (in (decay-motivation {:desire 0.0, :decay-rate 0.1})))

;; decay all motivations should decay each motivation in a sequence
(expect [{:name :hunger, :desire 0.1, :decay-rate 0.1, :max-change 0.3}]
        (decay-all-motivations [hunger]))

;; adding a percept should modify the motivations desire by the value in the
;; percept's satisfaction vector the corresponds to the motivation
(expect {:desire 0.5} (in (add-percept hunger percept)))

;; add-percepts should add all percepts to all motivations
(expect [{:name :hunger, :desire 0.5, :decay-rate 0.1, :max-change 0.3}]
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

;; desire should not change if it has changed less than :max-change
(expect {:desire 0.5} (in (limit-desire-change {:desire 0.5
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

;; max-change should be incremented when desire changes more than max-change
(expect (< 0.3
           (:max-change (limit-desire-change {:desire 0.9
                                              :last-desire 0.4
                                              :max-change 0.3}))))

(expect (< 0.3
           (:max-change (limit-desire-change {:desire 0.0
                                              :last-desire 0.4
                                              :max-change 0.3}))))

;; if defined, max-change-delta from motivation should be used to
;;adjust max-change
(expect (< 0.32
           (:max-change (limit-desire-change {:desire 0.9
                                              :last-desire 0.4
                                              :max-change 0.3
                                              :max-change-delta 0.3}))))

(expect (< 0.32
           (:max-change (limit-desire-change {:desire 0.0
                                              :last-desire 0.4
                                              :max-change 0.3
                                              :max-change-delta 0.3}))))
