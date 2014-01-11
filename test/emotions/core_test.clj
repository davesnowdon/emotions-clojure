(ns emotions.core-test
  (:require [emotions.core :refer :all]
            [expectations :refer :all]))

(def hunger {:desire 0.0 :decay 0.1})

(expect 0.1 (:desire (decay hunger)))
