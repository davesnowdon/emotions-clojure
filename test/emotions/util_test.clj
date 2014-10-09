(ns emotions.util-test
  (:require [emotions.util :refer :all]
            [expectations :refer :all]))

;; if above min value bounded- works just like -
(expect 5 (bounded- 6 1 1))

;; if result is less than min-value, shoudl return min-value
(expect (float= 0.1 (bounded- 4.5 4.45 0.1)))

;; zero values should be removed from a map
(expect {:joy 0.5 :hunger 0.2}
        (in (strip-zero {:joy 0.5 :anger 0.0 :hunger 0.2 :envy 0})))

;; values should be combined according to their weights
(expect (float= 0.75 (interpolate 0.5 1.0 1.0 1.0)))
(expect (float= 0.5 (interpolate 0.25 1.0 1.0 0.5)))
