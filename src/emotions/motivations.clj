(ns emotions.motivations)

; layers from bottom to top
(def layers [:physical :safety :social :skill :contribution])

;; valence and arousal for motivations organised by layers
(def default-motivations
  [{:id :phys-anger :name "anger" :layer :physical :valence -0.7 :arousal 0.7 }
   {:id :phys-fear :name "fear" :layer :physical :valence -0.9 :arousal 0.2}
   {:id :phys-pleasure :name "pleasure" :layer :physical :valence 0.7 :arousal 0.6}
   {:id :phys-disgust :name "disgust" :layer :physical :valence -0.4 :arousal 0.25}
   {:id :phys-attraction :name "attraction" :layer :physical :valence 0.5 :arousal 0.25}
   {:id :phys-bored :name "bored" :layer :physical :valence 0.0 :arousal -0.25}
   {:id :phys-sad :name "sad" :layer :physical :valence 0.6 :arousal -0.5}
   {:id :phys-calm :name "calm" :layer :physical :valence 0.4 :arousal -0.4}
   {:id :phys-tired :name "tired" :layer :physical :valence 0.0 :arousal -0.6},
   {:id :saf-rage :name "rage" :layer :safety :valence -0.8 :arousal 0.9}
   {:id :saf-playful :name "playful" :layer :safety :valence 0.6 :arousal 0.9}
   {:id :saf-gloat :name "gloat" :layer :safety :valence -0.3 :arousal 0.5}
   {:id :saf-delight :name "delight" :layer :safety :valence 0.7 :arousal 0.7 }
   {:id :saf-threatened :name "threatened" :layer :safety :valence -0.7 :arousal 0.4 }
   {:id :saf-envy :name "envy" :layer :safety :valence -0.6 :arousal 0.3}
   {:id :saf-greed :name "greed" :layer :safety :valence -0.1 :arousal 0.3}
   {:id :saf-hope :name "hope" :layer :safety :valence 0.4 :arousal 0.3}
   {:id :saf-giving :name "giving" :layer :safety :valence 0.8 :arousal 0.2}
   {:id :saf-anxiety :name "anxiety" :layer :safety :valence -0.5 :arousal 0.1}
   {:id :saf-relief :name "relief" :layer :safety :valence 0.2 :arousal -0.1}
   {:id :saf-disappointed :name "disappointed" :layer :safety :valence -0.5 :arousal -0.3}
   {:id :saf-bored :name "bored" :layer :safety :valence -0.1 :arousal -0.4}
   {:id :saf-dread :name "dread" :layer :safety :valence -0.7 :arousal -0.8}
   {:id :saf-lazy :name "lazy" :layer :safety :valence 0.0 :arousal -0.7}
   {:id :saf-content :name "content" :layer :safety :valence 0.6 :arousal -0.6},
   {:id :soc-hate :name "hate" :layer :social :valence -0.7 :arousal 0.8}
   {:id :soc-contempt :name "contempt" :layer :social :valence -0.6 :arousal 0.5}
   {:id :soc-pride :name "pride" :layer :social :valence 0.1 :arousal 0.6}
   {:id :soc-amity :name "amity" :layer :social :valence 0.7 :arousal 0.6}
   {:id :soc-jealousy :name "jealousy" :layer :social :valence -0.6 :arousal 0.4}
   {:id :soc-sympathy :name "sympathy" :layer :social :valence 0.4 :arousal 0.25}
   {:id :soc-embarrased :name "embarrased" :layer :social :valence -0.4 :arousal 0.1}
   {:id :soc-supportive :name "supportive" :layer :social :valence 0.5 :arousal -0.2}
   {:id :soc-shame :name "shame" :layer :social :valence -0.8 :arousal -0.4}
   {:id :soc-accepted :name "accepted" :layer :social :valence 0.6 :arousal -0.4}
   {:id :soc-lonely :name "lonely" :layer :social :valence -0.6 :arousal -0.6}
   {:id :soc-humility :name "humility" :layer :social :valence 0.4 :arousal -0.6},
   {:id :skill-revenge :name "revenge" :layer :skill :valence -0.7 :arousal 0.8}
   {:id :skill-courage :name "courage" :layer :skill :valence 0.2 :arousal 0.8}
   {:id :skill-victory :name "victory" :layer :skill :valence 0.6 :arousal 0.75}
   {:id :skill-arrogance :name "arrogance" :layer :skill :valence -0.1 :arousal 0.6}
   {:id :skill-revulsion :name "revulsion" :layer :skill :valence -0.7 :arousal 0.5}
   {:id :skill-significance :name "significance" :layer :skill :valence 0.6 :arousal 0.55}
   {:id :skill-honour :name "honour" :layer :skill :valence 0.1 :arousal 0.3}
   {:id :skill-attainment :name "attainment" :layer :skill :valence 0.6 :arousal 0.2}
   {:id :skill-useless :name "useless" :layer :skill :valence -0.6 :arousal -0.1}
   {:id :skill-pity :name "pity" :layer :skill :valence -0.1 :arousal -0.4}
   {:id :skill-guilt :name "guilt" :layer :skill :valence -0.8 :arousal -0.5}
   {:id :skill-fulfilled :name "fulfilled" :layer :skill :valence 0.6 :arousal -0.5}
   {:id :skill-remorse :name "remorse" :layer :skill :valence -0.7 :arousal -0.7},
   {:id :contrib-wrath :name "wrath" :layer :contribution :valence -0.9 :arousal 0.9}
   {:id :contrib-joy :name "joy" :layer :contribution :valence 0.8 :arousal 0.85}
   {:id :contrib-love :name "love" :layer :contribution :valence 0.8 :arousal 0.5}
   {:id :contrib-austere :name "austere" :layer :contribution :valence -0.4 :arousal 0.35}
   {:id :contrib-unity :name "unity" :layer :contribution :valence 0.8 :arousal 0.2}
   {:id :contrib-awe :name "awe" :layer :contribution :valence 0.3 :arousal -0.15}
   {:id :contrib-resignation :name "resignation" :layer :contribution :valence -0.4 :arousal -0.3}
   {:id :contrib-peace :name "peace" :layer :contribution :valence 0.7 :arousal -0.5}
   {:id :contrib-despair :name "despair" :layer :contribution :valence -0.8 :arousal -0.9}
   {:id :contrib-serenity :name "serenity" :layer :contribution :valence 0.8 :arousal -0.9}])
