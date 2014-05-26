(ns emotions.util)

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
