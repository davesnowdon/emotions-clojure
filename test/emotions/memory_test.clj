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
          :other-agents #{:joe}
          :locations #{:london}}
      p2 {:id (uuid)
          :name "Angry"
          :timestamp (t/now)
          :other-agents #{:joe}
          :locations #{:london}}]
  (expect true (equivalent-percepts p1 p2)))

;; Different percepts can't match even if other details are the same
(let [template {:timestamp (java.util.Date.)
                :other-agents #{:joe}
                :locations #{:london}}
      p1 (assoc template :id (uuid) :name "Foo")
      p2 (assoc template :id (uuid) :name "Bar")]
  (expect false (equivalent-percepts p1 p2)))

;; Percepts can't match if locations are different
(let [template {:name "Angry"
                :timestamp (t/now)
                :other-agents #{:joe}}
      p1 (assoc template :id (uuid) :locations #{:london})
      p2 (assoc template :id (uuid) :locations #{:paris})]
  (expect false (equivalent-percepts p1 p2)))

;; Percepts can't match if other agents are different
(let [template {:name "Angry"
                :timestamp (t/now)
                :locations #{:london}}
      p1 (assoc template :id (uuid) :other-agents #{:joe})
      p2 (assoc template :id (uuid) :other-agents #{:fred})]
  (expect false (equivalent-percepts p1 p2)))

;; adding a percept to short-term memory should increase the number of
;; percepts in memory
(let [stm #{}
      global-sv {:hunger 0.5, :survival 0.0}
      percept {:id (uuid)
               :name "Angry"
               :timestamp (t/now)
               :other-agents #{:joe}
               :locations #{:london}}
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
               :other-agents #{:joe}
               :locations #{:london}}
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
                :other-agents #{:joe}
                :locations #{:london}}
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
              :other-agents #{:joe}
              :locations #{:london}
              :stm-entry (t/minus (t/now) (t/millis 20000))
              :stm-expiration (t/minus (t/now) (t/millis 10000))}}]
  (expect 0 (count (:stm (short-term-memory-expired stm (t/now)))))
  (expect 1 (count (:expired (short-term-memory-expired stm (t/now))))))

;; each motivation has a learning window (lw) measured in milliseconds
;; over which updates to the learning vector are distributed. Each
;; update to the learning vector is weighted as
;; update interval * 1000 /  lw
;; lw milliseconds after the percept has entered short-term memory no
;; further updates are made to the learning vector
(let [motivations [{:id :phys-hunger :name "hunger" :layer :physical
                    :valence 0.0 :arousal 0.5
                    :desire 0.1 :decay-rate 0.0 :max-delta 1.0
                    :learning-window (* 2 60 60 1000)}
                   {:id :saf-delight :name "delight" :layer :safety
                    :valence 0.7 :arousal 0.7
                    :desire 0.4 :decay-rate 0.0 :max-delta 0.8
                    :learning-window 60000}]
      start-global-sv {:phys-hunger 0.1 :saf-delight 0.5}
      hungier-global-sv {:phys-hunger 0.9 :saf-delight 0.5}
      delighted-global-sv {:phys-hunger 0.1 :saf-delight 0.0}
      stm-entry (t/minus (t/now) (t/millis 10000))
      now (t/now)
      percepts [{:id (uuid)
                 :name "Angry"
                 :timestamp (t/now)
                 :other-agents #{:joe}
                 :locations #{:london}}]
      retain-period (t/millis 60000)
      stm (short-term-memory-add
           #{} percepts start-global-sv equivalent-percepts
           retain-period stm-entry)
      stm-no-change (short-term-memory-learn
                     stm start-global-sv motivations now)
      percept-no-change (first stm-no-change)
      stm-hungrier (short-term-memory-learn
                    stm hungier-global-sv motivations now)
      percept-hungrier (first stm-hungrier)
      stm-happier (short-term-memory-learn
                   stm delighted-global-sv motivations now)
      percept-happier (first stm-happier)]
  (expect (every? zero? (vals (:learning-vector percept-no-change))))
  (expect (not (every? zero? (vals (:learning-vector percept-hungrier)))))
  (expect (not (every? zero? (vals (:learning-vector percept-happier)))))
  )

;; should be able to check whether a percept is worth persisting to LTM
;; a percept where the mean of the abs of the learning vector is
;; greater than ltm-default-learning-vector-threshold should be
;; significant
(expect (percept-significant?
         {:learning-vector {:a 0.2 :b 0.0 :c 0.2 :d 0.0 :e 0.15}}))

;; a percept with a learning vector with a single value greater than
;; ltm-default-learning-vector-element-threshold should be significant
(expect (percept-significant?
         {:learning-vector {:a 0.0 :b 0.0 :c 0.0 :d 0.0 :e 0.31}}))

;; test simple add of new percept (to empty LTM)
(let [ltm (long-term-memory-init)
      percept-id (uuid)
      percept {:id percept-id
               :name "Angry"
               :timestamp (t/now)
               :other-agents #{{:id :joe :name "Joe"}}
               :locations #{{:id :london :name "London"}}
               :satisfaction-vector {:hunger 0.0, :survival 0.0 :joy 0.0}}
      ltm (-> (long-term-memory-init)
              (long-term-memory-add-percept  percept))]
  ;; should be able to save a new percept to LTM
  (expect 1 (count (:percepts ltm)))

  ;; time added to LTM should be stored
  (expect (:ltm-entry (first (vals (:percepts ltm)))))
  ;; new enrties (percepts, locations & agents) should have a modification
  ;; count of 1
  (expect 1 (:ltm-update-count (first (vals (:percepts ltm)))))

  ;; new locations should be stored in LTM (with satisfaction vector)
  (expect 1 (count (:locations ltm)))
  (expect (:satisfaction-vector (first (vals (:locations ltm)))))

  ;; new agents should be stored in LTM (with satisfaction vector)
  (expect 1 (count (:agents ltm)))
  (expect (:satisfaction-vector (first (vals (:agents ltm)))))
  )

;; should be able to update an existing percept in LTM
(let [template {:id (uuid)
                :name "Angry"
                :timestamp (t/now)
                :other-agents #{:joe}
                :locations #{:london}}
      old-sv {:hunger 0.0, :survival 0.0 :joy 0.0}
      old-percept (assoc template :satisfaction-vector old-sv)
      new-percept (assoc template
                    :satisfaction-vector
                    {:hunger 0.0, :survival 0.5 :joy 0.0}
                    :learning-vector
                    {:hunger 0.1, :survival -0.2 :joy 0.1})
      ltm (-> (long-term-memory-init)
              (long-term-memory-add-percept old-percept)
              (long-term-memory-add-percept new-percept))
      new-sv (:satisfaction-vector (first (vals (:percepts ltm))))]
  (expect (not (= old-sv new-sv)))
  )

;; satisfaction vectors for locations should be updated
;; TODO

;; satisfaction vectors for agents should be updated
;; TODO

;; should be able to look up a percept in long-term memory and get
;; a satisfaction vector
;;
;; a percept that matches exactly should have a weight of 1.0
(let [sv {:hunger 0.5, :survival 0.0}
      percept {:id (uuid)
               :name "Angry"
               :timestamp (t/now)
               :other-agents #{:joe}
               :locations #{:london}
               :satisfaction-vector sv}
      ltm (long-term-memory-add-percept
           (long-term-memory-init) percept)]
  (expect {:satisfaction-vector sv :weight 1.0}
          (long-term-memory-get-sv ltm percept)))

;; if there is no exact match, returned value and weight based on
;; closest match based on name, location & other agents
(let [percepts [{:id (uuid)
                 :name "Happy"
                 :timestamp (t/now)
                 :other-agents #{:joe}
                 :locations #{:london}
                 :satisfaction-vector {:hunger 0.0, :survival 0.0 :joy 0.0}}
                {:id (uuid)
                 :name "Angry"
                 :timestamp (t/now)
                 :other-agents #{:joe :dave}
                 :locations #{:london :home}
                 :satisfaction-vector {:hunger 0.0 :survival 0.5 :joy 0.8}}
                {:id (uuid)
                 :name "Sad"
                 :timestamp (t/now)
                 :other-agents #{:carl}
                 :locations #{:paris}
                 :satisfaction-vector {:hunger 0.0 :survival 0.0 :joy 0.8}}]
      ; put a sequence of percepts into LTM
      ltm (reduce long-term-memory-add-percept (long-term-memory-init) percepts)]
  ; exact match should give us max weight
  (expect {:weight 1.0 :satisfaction-vector {:hunger 0.0, :survival 0.0 :joy 0.0}}
          (long-term-memory-get-sv ltm {:name "Happy"
                                        :other-agents #{:joe}
                                        :locations #{:london}}))

  ; name only match should give us weight of name only
  (expect {:weight 1/3 :satisfaction-vector {:hunger 0.0, :survival 0.0 :joy 0.0}}
          (long-term-memory-get-sv ltm {:name "Happy"
                                        :other-agents #{:fred}
                                        :locations #{:tokyo}}))

  ; other-agents and name should give us more weight
  (expect {:weight 2/3 :satisfaction-vector {:hunger 0.0, :survival 0.0 :joy 0.0}}
          (long-term-memory-get-sv ltm {:name "Happy"
                                        :other-agents #{:joe}
                                        :locations #{:tokyo}}))

  ; location and name should give us more weight
  (expect {:weight 2/3 :satisfaction-vector {:hunger 0.0, :survival 0.5 :joy 0.8}}
          (long-term-memory-get-sv ltm {:name "Angry"
                                        :other-agents #{:carl}
                                        :locations #{:london}}))

  ; one out of 2 locations should be half the weight of getting all locations
  (expect
   (float= (float 1/6)
           (:weight
            (long-term-memory-get-sv ltm {:name "Miserable"
                                          :other-agents #{:fred}
                                          :locations #{:london :paris}}))))

  )
