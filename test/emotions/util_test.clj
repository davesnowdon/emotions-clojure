(ns emotions.util-test
  (:require [emotions.util :refer :all]
            [expectations :refer :all]))

;; if above min value bounded- works just like -
(expect 5 (bounded- 6 1 1))

;; if result is less than min-value, shoudl return min-value
(expect (float= 0.1 (bounded- 4.5 4.45 0.1)))
