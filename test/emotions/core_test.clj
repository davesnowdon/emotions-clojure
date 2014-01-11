(ns emotions.core-test
  (:require [emotions.core :refer :all]
            [expectations :refer :all]))

(def hunger {:name :hunger, :desire 0.0, :decay-rate 0.1})

(def percept {:satisfaction-vector {:hunger 0.5, :survival 0.0}})

;; desire should increase by value of :decay-rate
(expect 0.1 (:desire (decay-motivation {:desire 0.0, :decay-rate 0.1})))

(expect [{:name :hunger, :desire 0.1, :decay-rate 0.1}]
        (decay-all-motivations [hunger]))

(expect 0.5 (:desire (add-percept hunger percept)))

(expect [{:name :hunger, :desire 0.5, :decay-rate 0.1}]
        (add-percepts [hunger] [percept]))
