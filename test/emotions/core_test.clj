(ns emotions.core-test
  (:require [emotions.core :refer :all]
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
