(ns emotions.memory-test
  (:require [emotions.core :refer :all]
            [emotions.util :refer :all]
            [emotions.motivations :refer :all]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [expectations :refer :all]))

;; percepts are equivalent if the name, agents and locations match
(let [p1 {:id (uuid)
          :name "Angry"
          :timestamp (t/now)
          :other-agents [:joe]
          :locations [:london]}
      p2 {:id (uuid)
          :name "Angry"
          :timestamp (t/now)
          :other-agents [:joe]
          :locations [:london]}]
  (expect true (equivalent-percepts p1 p2)))

;; Different percepts can't match even if other details are the same
(let [template {:timestamp (java.util.Date.)
                :other-agents [:joe]
                :locations [:london]}
      p1 (assoc template :id (uuid) :name "Foo")
      p2 (assoc template :id (uuid) :name "Bar")]
  (expect false (equivalent-percepts p1 p2)))

;; Percepts can't match if locations are different
(let [template {:name "Angry"
                :timestamp (t/now)
                :other-agents [:joe]}
      p1 (assoc template :id (uuid) :locations [:london])
      p2 (assoc template :id (uuid) :locations [:paris])]
  (expect false (equivalent-percepts p1 p2)))

;; Percepts can't match if other agents are different
(let [template {:name "Angry"
                :timestamp (t/now)
                :locations [:london]}
      p1 (assoc template :id (uuid) :other-agents [:joe])
      p2 (assoc template :id (uuid) :other-agents [:fred])]
  (expect false (equivalent-percepts p1 p2)))

;; adding a percept to short-term memory should increase the number of
;; percepts in memory
(let [stm #{}
      global-sv {:hunger 0.5, :survival 0.0}
      percept {:id (uuid)
               :name "Angry"
               :timestamp (t/now)
               :other-agents [:joe]
               :locations [:london]}
      retain-period (t/millis 10000)]
  (expect 1 (count (short-term-memory-add stm
                                          [percept]
                                          global-sv
                                          equivalent-percepts
                                          retain-period))))

;; adding a percept to short-term memory should give it an expiration time
;; and a learning vector
(let [stm #{}
      global-sv {:hunger 0.5, :survival 0.0}
      percept {:id (uuid)
               :name "Angry"
               :timestamp (t/now)
               :other-agents [:joe]
               :locations [:london]}
      retain-period (t/millis 10000)
      new-stm (short-term-memory-add stm
                                     [percept]
                                     global-sv
                                     equivalent-percepts
                                     retain-period)]
  (expect (:stm-entry (first new-stm)))
  (expect (:stm-expiration (first new-stm)))
  (expect (set (keys (:learning-vector (first new-stm))))
          (set (keys global-sv)))
  (expect (:satisfaction-vector-obs (first new-stm)) global-sv)
  )

;; if a similar percept is in short-term memory then the new percept is
;; not added and the expiration time of the existing percept is extended
(let [old-exp (t/now)
      global-sv {:hunger 0.5, :survival 0.0}
      template {:name "Angry"
                :other-agents [:joe]
                :locations [:london]}
      stm #{(assoc template
              :timestamp (t/minus (t/now) (t/millis 10000))
              :stm-expiration old-exp)}
      percept (assoc template :timestamp (t/now))
      retain-period (t/millis 10000)
      new-stm (short-term-memory-add stm
                                     [percept]
                                     global-sv
                                     equivalent-percepts
                                     retain-period)]
  (expect 1 (count new-stm))
  (expect (t/after? (:stm-expiration (first new-stm)) old-exp)))

;; should return short-term memory without expired percepts
(let [stm #{ {:id (uuid)
              :name "Got angry"
              :other-agents [:joe]
              :locations [:london]
              :timestamp (t/minus (t/now) (t/millis 20000))
              :stm-expiration (t/minus (t/now) (t/millis 10000))}}]
  (expect 0 (count (:stm (short-term-memory-expired stm (t/now)))))
  (expect 1 (count (:expired (short-term-memory-expired stm (t/now))))))

;; each motivation has a learning window (lw) measured in milliseconds over which
;; updates to the learning vector are distributed. Each update to the learning vector
;; is weighted as update interval * 1000 /  lw
;; lw milliseconds after the percept has entered short-term memory no further
;; updates are made to the learning vector

;(short-term-memory-learn stm interval global-sv motivations)
