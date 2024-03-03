(ns verlet-typed-cljs.slow
  (:require [verlet-typed-cljs.state :refer [state]]
            [verlet-typed-cljs.utils :refer [rand-range add-particle]]))

; SLOW first ver

(defn- offset [i] (* i (:size-p @state)))

(defn- apply-force
  [state i [fx fy]]
  (let [pos (:particles @state)
        idx (offset i)
        ax (aget pos (+ idx 4))
        ay (aget pos (+ idx 5))]
    (aset pos (+ idx 4) (+ ax fx))
    (aset pos (+ idx 5) (+ ay fy)))
  state)

(defn- collide-with!
  [state i]
  (let [pos (:particles @state)
        part-a (.slice pos (offset i) (+ (offset i) (:size-p @state)))
        xa (aget part-a 0)
        ya (aget part-a 1)
        rad (:radius @state)
        min-dist (+ rad rad)]
    (loop [j 0]
      (when (and (not (= i j)) (<= j (:num-particles @state)))
        (let [part-b (.slice pos (offset j) (+ (offset j) (:size-p @state)))
              xb (aget part-b 0)
              yb (aget part-b 1)
              vx (- xa xb)
              vy (- ya yb)
              dist-sq (+ (* vx vx) (* vy vy))]
          (when (< dist-sq (* min-dist min-dist))
            (let [dist (js/Math.sqrt dist-sq)
                  factor (* (/ (- dist min-dist) dist) 0.5)
                  dix (* vx factor)
                  diy (* vy factor)]
              (aset pos (offset i) (- xa dix))
              (aset pos (inc (offset i)) (- ya diy))
              (aset pos (offset j) (+ xb dix))
              (aset pos (inc (offset j)) (+ yb diy)))))
        (recur (inc j))))))

(defn- collide
  [state]
  (dotimes [i (:num-particles @state)] (collide-with! state i)))

(defn- bounce
  [state]
  (let [pos (:particles @state)
        width (:width @state)
        height (:height @state)
        radius (:radius @state)]
    (dotimes [i (:num-particles @state)]
      (let [idx (offset i)
            part (.slice pos idx (+ idx (:size-p @state)))
            x (aget part 0)
            y (aget part 1)
            px (aget part 2)
            py (aget part 3)
            dx (- px x)
            dy (- py y)]
        (cond (< x radius) (aset pos (offset i) (+ x (* 1.8 dx)))
              (> x (- width radius)) (aset pos (offset i) (+ x (* 1.8 dx)))
              (< y radius) (aset pos (inc (offset i)) (+ y (* 1.8 dy)))
              (> y (- height radius))
                (aset pos (inc (offset i)) (+ y (* 1.8 dy))))))))

(defn- accelerate-particles
  [state i dt]
  (let [pos (:particles @state)
        idx (offset i)
        dt-sq (* dt dt)
        px (aget pos (+ idx 2))
        py (aget pos (+ idx 3))
        ax (* (aget pos (+ idx 4)) dt-sq)
        ay (* (aget pos (+ idx 5)) dt-sq)]
    (aset pos (+ idx 2) (+ px ax))
    (aset pos (+ idx 3) (+ py ay))
    (aset pos (+ idx 4) 0)
    (aset pos (+ idx 5) 0))
  state)

(defn- update-particles
  [state i]
  (let [pos (:particles @state)
        idx (offset i)
        x (aget pos (+ idx 0))
        y (aget pos (+ idx 1))
        px (aget pos (+ idx 2))
        py (aget pos (+ idx 3))]
    (.set pos #js [(- (* x 2) px) (- (* y 2) py) x y] idx)))

(defn- draw-particles
  [state]
  (let [pos (:particles @state)
        ctx (:ctx @state)
        width (.-width (.-canvas ctx))
        height (.-height (.-canvas ctx))
        radius (:radius @state)]
    (.clearRect ctx 0 0 width height)
    (dotimes [i (:num-particles @state)]
      (let [x (aget pos (offset i))
            y (aget pos (inc (offset i)))]
        (.beginPath ctx)
        (.arc ctx x y radius 0 (* 2 Math/PI) false)
        (.fill ctx)))))

(defn- update-all
  [state dt]
  (let [gravity (:gravity @state)]
    (dotimes [i (:num-particles @state)]
      (-> state
          (apply-force i gravity)
          (accelerate-particles i dt)
          (update-particles i)))))

(defn init
  [state]
  (println "init slow")
  (let [ctx (:ctx @state)
        radius (:radius @state)
        num-particles (:num-particles @state)
        width (.-width (.-canvas ctx))
        height (.-height (.-canvas ctx))
        size-p (:size-p @state)]
    (swap! state assoc :particles (js/Float32Array. (* num-particles size-p)))
    (dotimes [i num-particles]
      (add-particle state
                    (rand-range radius (- width radius))
                    (rand-range radius (- height radius))
                    (* i size-p)))))

(defn run
  [dt]
  (update-all state dt)
  (collide state)
  (bounce state)
  (draw-particles state))
