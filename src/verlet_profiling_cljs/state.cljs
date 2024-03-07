(ns verlet-profiling-cljs.state
  (:require [reagent.core :as r]))

(defonce settings
  {:running? false,
   :num-particles 3,
   :radius 10,
   :gravity [0 -0.98],
   :dimensions 2,
   :width 900,
   :height 900,
   :mode :array-buffer})

(defonce state (r/atom (merge settings {:size-p (* (:dimensions settings) 3)})))
