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

(def size-p (* (:dimensions settings) 3)) ; (x y px py ax ay)

(def particles (js/Float32Array. (* (:num-particles settings) size-p)))

(def buffer (.-buffer particles))

(defn reset-state!
  []
  (merge settings {:size-p size-p, :particles particles, :buffer buffer}))

(defonce state (r/atom (reset-state!)))

; (defonce state (reset-state!))
