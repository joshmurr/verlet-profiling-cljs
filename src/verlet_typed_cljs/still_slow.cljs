(ns verlet-typed-cljs.still-slow
  (:require [verlet-typed-cljs.state :refer [state]]))

; "SHOULD BE FASTER BUT IS STILL SLOW"

; Turns out this isn't toooo bad, but Fighweel adds a lot of overhead
; which slows everything down. The :advanced compiled version speeds
; things up A LOT. From a 40ms RAF to 6ms or something like that.
; So I think the reason why this is slower than the original
; (slow.cljs) is because there are more function calls. And maybe:
; 1. that is just slow in an unoptimised way?
; 2. Figwheel just has a lot of fingers in a lot of pies?
; 3. All this shit gets inlined when optimised so no need to worry.
;
; So! If I can get it running well *without* optimisations... then
; it'll be pretty rapid *with* optimisations, right?

(defn offset [i] (* i (:size-p @state)))

(defn get-pos
  [particles idx]
  [(aget particles (+ idx 0)) (aget particles (+ idx 1))])
(defn get-prev-pos
  [particles idx]
  [(aget particles (+ idx 2)) (aget particles (+ idx 3))])
(defn get-acc
  [particles idx]
  [(aget particles (+ idx 4)) (aget particles (+ idx 5))])

(defn set-pos!
  [particles idx x y]
  (aset particles (+ idx 0) x)
  (aset particles (+ idx 1) y))
(defn set-prev-pos!
  [particles idx x y]
  (aset particles (+ idx 2) x)
  (aset particles (+ idx 3) y))
(defn set-acc!
  [particles idx x y]
  (aset particles (+ idx 4) x)
  (aset particles (+ idx 5) y))

(defn apply-force
  [particles idx [fx fy]]
  (let [[ax ay] (get-acc particles idx)]
    (set-acc! particles idx (+ ax fx) (+ ay fy)))
  particles)


(defn accelerate-particle
  [particles idx dt]
  (let [dt-sq (* dt dt)
        [px py] (get-prev-pos particles idx)
        [ax ay] (get-acc particles idx)]
    (set-prev-pos! particles idx (+ px (* ax dt-sq)) (+ py (* ay dt-sq)))
    (set-acc! particles idx 0 0))
  particles)

(defn update-particle
  [particles idx]
  (let [[x y] (get-pos particles idx)
        [px py] (get-prev-pos particles idx)]
    (set-pos! particles idx (- (* 2 x) px) (- (* 2 y) py))
    (set-prev-pos! particles idx x y)))

(defn update-and-accelerate
  [particles idx dt]
  (let [dt-sq (* dt dt)
        [x y] (get-pos particles idx)
        [px py] (get-prev-pos particles idx)
        [ax ay] (get-acc particles idx)]
    (set-pos! particles
              idx
              (- (* 2 x) (+ px (* ax dt-sq)))
              (- (* 2 y) (+ py (* ay dt-sq))))
    (set-prev-pos! particles idx x y))
  (set-acc! particles idx 0 0))


(defn draw-particles
  [state]
  (let [ctx (:ctx @state)
        width (.-width (.-canvas ctx))
        height (.-height (.-canvas ctx))
        radius (:radius @state)
        num-particles (:num-particles @state)
        particles (:particles @state)]
    (.clearRect ctx 0 0 width height)
    (dotimes [i num-particles]
      (let [[x y] (get-pos particles (offset i))]
        (.beginPath ctx)
        (.arc ctx x y radius 0 (* 2 Math/PI) false)
        (.fill ctx)))))

(defn update-all
  [state dt]
  (let [particles (:particles @state)
        num-particles (:num-particles @state)
        gravity (:gravity @state)]
    (dotimes [i num-particles]
      (-> particles
          (apply-force (offset i) gravity)
          #_(update-and-accelerate (offset i) dt)
          (accelerate-particle (offset i) dt)
          (update-particle (offset i))))))

(defn bounce-all
  [state]
  (let [ctx (:ctx @state)
        width (.-width (.-canvas ctx))
        height (.-height (.-canvas ctx))
        particles (:particles @state)
        radius (:radius @state)
        num-particles (:num-particles @state)]
    (dotimes [i num-particles]
      (let [idx (offset i)
            [x y] (get-pos particles idx)
            [px py] (get-prev-pos particles idx)
            dx (- px x)
            dy (- py y)]
        (cond (< x radius) (aset particles (+ idx 0) (+ x (* 2 dx)))
              (> x (- width radius)) (aset particles (+ idx 0) (+ x (* 2 dx)))
              (< y radius) (aset particles (+ idx 1) (+ y (* 2 dy)))
              (> y (- height radius))
                (aset particles (+ idx 1) (+ y (* 2 dy))))))))

(defn collide-all
  [state]
  (let [particles (:particles @state)
        num-particles (:num-particles @state)
        rad (:radius @state)
        min-dist (+ rad rad)]
    (dotimes [i num-particles]
      (let [idxa (offset i)
            [xa ya] (get-pos particles idxa)]
        (loop [j 0]
          (when (and (not (= i j)) (<= j num-particles))
            (let [idxb (offset j)
                  [xb yb] (get-pos particles idxb)
                  vx (- xa xb)
                  vy (- ya yb)
                  dist-sq (+ (* vx vx) (* vy vy))]
              (when (< dist-sq (* min-dist min-dist))
                (let [dist (js/Math.sqrt dist-sq)
                      factor (* (/ (- dist min-dist) dist) 0.5)
                      dix (* vx factor)
                      diy (* vy factor)]
                  (set-pos! particles idxa (- xa dix) (- ya diy))
                  (set-pos! particles idxb (+ xb dix) (+ yb diy)))))
            (recur (inc j))))))))

(defn run
  []
  (do (update-all state (/ (:dt @state) 1000))
      (collide-all state)
      (bounce-all state)
      (draw-particles state)))
