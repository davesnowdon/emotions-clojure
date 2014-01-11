(ns emotions.core-test
  (:require [emotions.core :refer :all]
            [expectations :refer :all]))

(def hunger {:name :hunger, :desire 0.0, :decay-rate 0.1})

(def percept {:satisfaction-vector {:hunger 0.5}})

;; desire should increase by value of :decay-rate
(expect 0.1 (:desire (decay {:desire 0.0, :decay-rate 0.1})))

(expect 0.5 (:desire (add-percept hunger percept)))
