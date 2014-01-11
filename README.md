emotions-clojure
================

Clojure implementation of emotional model from github.com/davesnowdon/emotions

A model of emotions and motivations for artificial agents inspired by the work of Michael Sellers as described in "Toward a comprehensive theory of emotion for biological and artificial agents" in Biologically Inspired Cognitive Architectures, Volume 4, April 2013, Pages 3–26.


Motivation - roughly analgous to an emotion; processes perceptions and creates a score representing the level of "activation" of this emotion

Satisfaction vector (SV) - contains a score for each motivation. These are modeled as python dictionaries with the keys being the identifier (string) of a motivation and the numbers (the output of the motivation functions).

Percept - A piece of sense data (internal or external) that is "perceived" by one or more motivation.

Long term memory (LTM) - long lived association of percepts and their associated satisfaction vectors

Short term memory (STM) - recently perceived percepts and their associated satisfaction vectors
