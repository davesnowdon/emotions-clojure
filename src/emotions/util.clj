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
