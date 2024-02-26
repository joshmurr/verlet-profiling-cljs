(ns verlet-typed-cljs.state
  (:require [reagent.core :as r]))

(defonce settings
  {:num-particles 100,
   :radius 4,
   :gravity [0 -5.8],
   :drag 0.1,
   :mass 1,
   :dimensions 2,
   :width 900,
   :height 900,
   :t 0,
   :moving-avg-window 50,
   :dt 0,
   :samples [],
   :moving-avg 0,
   :mode "still-slow"})

; slow 15
; slower 14
; still-slow 20

(defonce state (r/atom (merge settings {:size-p (* (:dimensions settings) 3)})))
