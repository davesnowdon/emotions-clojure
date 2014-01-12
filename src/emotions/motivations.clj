(ns emotions.motivations)

(def layers [:physical :safety :social :values :contribution])

;; valence and arousal for motivations organised by layers
(def motivations-by-layers
  {:physical {:anger {:valence -0.7 :arousal 0.7 }
              :fear {:valence -0.9 :arousal 0.2}
              :pleasure {:valence 0.7 :arousal 0.6}
              :disgust {:valence -0.4 :arousal 0.25}
              :attraction {:valence 0.5 :arousal 0.25}
              :bored {:valence 0.0 :arousal -0.25}
              :sad {:valence 0.6 :arousal -0.5}
              :calm {:valence 0.4 :arousal -0.4}
              :tired {:valence 0.0 :arousal -0.6}},
   :safety {:rage {:valence -0.8 :arousal 0.9}
            :playful {:valence 0.6 :arousal 0.9}
            :gloat {:valence -0.3 :arousal 0.5}
            :delight {:valence 0.7 :arousal 0.7 }
            :threatened {:valence -0.7 :arousal 0.4 }
            :envy {:valence -0.6 :arousal 0.3}
            :greed {:valence -0.1 :arousal 0.3}
            :hope {:valence 0.4 :arousal 0.3}
            :giving {:valence 0.8 :arousal 0.2}
            :anxiety {:valence -0.5 :arousal 0.1}
            :relief {:valence 0.2 :arousal -0.1}
            :disappointed {:valence -0.5 :arousal -0.3}
            :bored {:valence -0.1 :arousal -0.4}
            :dread {:valence -0.7 :arousal -0.8}
            :lazy {:valence 0.0 :arousal -0.7}
            :content {:valence 0.6 :arousal -0.6}},
   :social {:hate {:valence -0.7 :arousal 0.8}
            :contempt {:valence -0.6 :arousal 0.5}
            :pride {:valence 0.1 :arousal 0.6}
            :amity {:valence 0.7 :arousal 0.6}
            :jealousy {:valence -0.6 :arousal 0.4}
            :sympathy {:valence 0.4 :arousal 0.25}
            :embarrased {:valence -0.4 :arousal 0.1}
            :supportive {:valence 0.5 :arousal -0.2}
            :shame {:valence -0.8 :arousal -0.4}
            :accepted {:valence 0.6 :arousal -0.4}
            :lonely {:valence -0.6 :arousal -0.6}
            :humility {:valence 0.4 :arousal -0.6}},
   :values {:revenge {:valence -0.7 :arousal 0.8}
            :courage {:valence 0.2 :arousal 0.8}
            :victory {:valence 0.6 :arousal 0.75}
            :arrogance {:valence -0.1 :arousal 0.6}
            :revulsion {:valence -0.7 :arousal 0.5}
            :significance {:valence 0.6 :arousal 0.55}
            :honour {:valence 0.1 :arousal 0.3}
            :attainment {:valence 0.6 :arousal 0.2}
            :useless {:valence -0.6 :arousal -0.1}
            :pity {:valence -0.1 :arousal -0.4}
            :guilt {:valence -0.8 :arousal -0.5}
            :fulfilled {:valence 0.6 :arousal -0.5}
            :remorse {:valence -0.7 :arousal -0.7}},
   :contribution {:wrath {:valence -0.9 :arousal 0.9}
                  :joy {:valence 0.8 :arousal 0.85}
                  :love {:valence 0.8 :arousal 0.5}
                  :austere {:valence -0.4 :arousal 0.35}
                  :unity {:valence 0.8 :arousal 0.2}
                  :awe {:valence 0.3 :arousal -0.15}
                  :resignation {:valence -0.4 :arousal -0.3}
                  :peace {:valence 0.7 :arousal -0.5}
                  :despair {:valence -0.8 :arousal -0.9}
                  :serenity {:valence 0.8 :arousal -0.9}}})
