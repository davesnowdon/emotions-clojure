(ns emotions.core)

(defn decay
  ""
  [motivation]
  (assoc motivation :desire (+ (:desire motivation) (:decay motivation))))
