(ns emotions.util
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]))

;; from http://gettingclojure.wikidot.com/cookbook:numbers
(defn float=
  ([x y] (float= x y 0.00001))
  ([x y epsilon]
     (let [scale (if (or (zero? x) (zero? y)) 1 (Math/abs x))]
       (<= (Math/abs (- x y)) (* scale epsilon)))) )

;; from http://blog.jayfields.com/2011/08/clojure-apply-function-to-each-value-of.html
(defn update-values
  "Apply a function to each value of a map"
  [m f & args]
 (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))

(defn bounded-
  "Subtracts the second argument from the first argument and returns the result unless it is smaller than min value in which case min-value is returned"
  [a b min-value]
  (let [result (- a b)]
    (if (< result min-value)
      min-value
      result)))

(defn strip-zero
  "Removes items from a map where the value is zero"
  [m]
  (->> m
       (filter (fn [[k v]] (not (float= 0.0 v))))
       (map (partial apply hash-map))
       (apply merge-with concat)))

;; from https://gist.github.com/gorsuch/1418850
(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn add-key
  "Add key & value to every map in a sequence"
  [ms key value]
  (map #(assoc % key value) ms))

(defn millis-diff
  "Return the difference between two DateTimes in milliseconds (long)"
  [dt1 dt2]
  (- (tc/to-long dt1) (tc/to-long dt2)))

(defn seconds-diff
  "Return the difference between two DateTimes as seconds (float)"
  [dt1 dt2]
  (/ (millis-diff dt1 dt2) (float 1000)))

(defn abs-all
  "Apply the abs function to each element in a collection"
  [num-coll]
  (map #(Math/abs %) num-coll))

(defn mean
  "Return the mean (average) of a collection of numbers"
  [num-coll]
  (/ (apply + num-coll) (count num-coll)))

(defn interpolate
  "Interpolate 2 values according to their relative weights"
  [a a-weight b b-weight]
  (let [w-sum (+ a-weight b-weight)
        a-frac (/ a-weight w-sum)
        b-frac (/ b-weight w-sum)]
    (+ (* a-frac a) (* b-frac b))))
