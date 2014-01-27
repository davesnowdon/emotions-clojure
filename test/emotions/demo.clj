(ns emotions.demo
  (:require [emotions.core :refer :all]
            [emotions.util :refer :all]))

; layers from bottom to top
(def demo-layers [:physical :safety :social :skill :contribution])

(def demo-motivations
  [{:id :phys-hunger :name "hunger" :layer :physical
    :valence 0.0 :arousal 0.5
    :desire 0.0 :decay-rate 0.1 :max-delta 1.0}
   {:id :phys-fear :name "fear" :layer :physical
    :valence -0.9 :arousal 0.2
    :desire 0.0 :decay-rate 0.0 :max-delta 1.0}
   {:id :saf-bored :name "bored" :layer :safety
    :valence -0.1 :arousal -0.4
    :desire 0.0 :decay-rate 0.1 :max-delta 0.3}
   {:id :saf-delight :name "delight" :layer :safety
    :valence 0.7 :arousal 0.7
    :desire 0.0 :decay-rate 0.0 :max-delta 0.8}
   {:id :soc-lonely :name "lonely" :layer :social
    :valence -0.6 :arousal -0.6
    :desire 0.0 :decay-rate 0.1 :max-delta 0.3}
   ])

(def demo-control-points
  [{:valence -1.0 :arousal 1.0 :expression-vector
    {:phys-hunger 0.5 :phys-fear 0.8 :saf-bored 0.0 :saf-delight 0.0 :soc-lonely 0.2}}

   {:valence 0.0 :arousal 1.0 :expression-vector
    {:phys-hunger 0.0 :phys-fear 0.0  :saf-bored 0.5 :saf-delight 0.8 :soc-lonely 0.0}}

   {:valence 1.0 :arousal 1.0 :expression-vector
    {:phys-hunger 0.0 :phys-fear 0.0 :saf-bored 0.0 :saf-delight 0.9 :soc-lonely 0.0}}

   {:valence -1.0 :arousal 0.0 :expression-vector
    {:phys-hunger 0.1 :phys-fear 0.5 :saf-bored 0.3 :saf-delight 0.0 :soc-lonely 0.1}}

   {:valence 0.0 :arousal 0.0 :expression-vector
    {:phys-hunger 0.5 :phys-fear 0.1 :saf-bored 0.8 :saf-delight 0.0 :soc-lonely 0.3}}

   {:valence 1.0 :arousal 0.0 :expression-vector
    {:phys-hunger 0.0 :phys-fear 0.0 :saf-bored 0.0 :saf-delight 0.5 :soc-lonely 0.0}}

   {:valence -1.0 :arousal -1.0 :expression-vector
    {:phys-hunger 0.0 :phys-fear 0.0 :saf-bored 0.3 :saf-delight 0.0 :soc-lonely 0.0}}

   {:valence 0.0 :arousal -1.0 :expression-vector
    {:phys-hunger 0.0 :phys-fear 0.0 :saf-bored 0.0 :saf-delight 0.0 :soc-lonely 0.0}}

   {:valence 1.0 :arousal -1.0 :expression-vector
    {:phys-hunger 0.0 :phys-fear 0.0 :saf-bored 0.0 :saf-delight 0.0 :soc-lonely 0.0}}
   ])

(defn select-percepts
  "Give the user a list of possible percepts and allow them to choose some"
  []
  [])

(defn display-sv
  "Display satisfaction vector"
  [sv]
  (println "SV:" sv))

(defn demo-instructions
  []
  (println "Select which percepts, or none to show at each time step to see how the satisfaction-vector changes. Selecting no percepts will mean that only motications with a decay rate will change."))

(defn continue-demo?
  []
  (do
    (println "Continue? (y/n)")
    (let [input (read-line)]
      (or (= "y" input) (= "Y" input)))))

(defn run-demo
  "Repeatedly evaluate input and display resulting satisfaction vector"
  []
  (let [ initial-sv (motivations->sv demo-motivations)
         layers demo-layers]
    (do
      (demo-instructions)
      (loop [sv initial-sv
             motivations demo-motivations]
        (do
          (display-sv sv)
          (if (continue-demo?)
            (let [percepts (select-percepts)
                  [new-motivations new-sv]
                  (percepts->motivations+sv layers motivations percepts)]
              (recur new-sv new-motivations))))))))

(defn -main
  [& args]
  (run-demo))
