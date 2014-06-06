(ns emotions.serialise
  (:require [emotions.util :refer :all]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.edn :as edn]))

(declare serialise)
(declare deserialise)

(defn- serialise-seq
  [v]
  (map serialise v))

(defn- deserialise-seq
  [v]
  (map deserialise v))

(defn- serialise-vector
  [v]
  (vec (serialise-seq v)))

(defn- deserialise-vector
  [v]
  (vec (deserialise-seq v)))

(defn- serialise-set
  [v]
  (set (serialise-seq v)))

(defn- deserialise-set
  [v]
  (set (deserialise-seq v)))

(defn- serialise-map
  [m]
  (reduce-kv (fn [m k v] (assoc m (serialise k) (serialise v)))
             (empty m) m))

(defn- deserialise-map
  [m]
  (reduce-kv (fn [m k v] (assoc m (deserialise k) (deserialise v)))
             (empty m) m))

(defn- serialise-value
  [v]
  (cond
   (instance? org.joda.time.DateTime v) (tc/to-date v)
   :else v))

(defn- deserialise-value
  [v]
  (cond
   (instance? java.util.Date v) (tc/from-date v)
   :else v))

(defn serialise
  [v]
  (cond
   (map? v) (serialise-map v)
   (vector? v) (serialise-vector v)
   (set? v) (serialise-set v)
   (seq? v) (serialise-seq v)
   :else (serialise-value v)))

(defn deserialise
  [v]
  (cond
   (map? v) (deserialise-map v)
   (vector? v) (deserialise-vector v)
   (set? v) (deserialise-set v)
   (seq? v) (deserialise-seq v)
   :else (deserialise-value v)))
