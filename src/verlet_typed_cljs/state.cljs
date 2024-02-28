(ns verlet-typed-cljs.state
  (:require [reagent.core :as r]))

(defonce settings
  {:running? false,
   :num-particles 2,
   :radius 10,
   :gravity [0 -0.8],
   :drag 0.1,
   :mass 1,
   :dimensions 2,
   :width 900,
   :height 900,
   :t 0,
   :dt 0.1,
   :moving-avg-window 50,
   :samples [],
   :moving-avg 0,
   :mode :still-slow})

; slow 15
; slower 14
; still-slow 20

(defonce state (r/atom (merge settings {:size-p (* (:dimensions settings) 3)})))
