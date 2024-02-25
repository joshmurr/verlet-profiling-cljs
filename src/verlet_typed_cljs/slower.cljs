(ns verlet-typed-cljs.slower
  (:require [verlet-typed-cljs.state :refer [state]]
            [verlet-typed-cljs.utils :refer [pset!]]))

(defn offset [i] (* i (:size-p @state)))
(defn byte-offset
  [i]
  (* i (:size-p @state) (. js/Float32Array -BYTES_PER_ELEMENT)))

(defn get-pos [particle] [(aget particle 0) (aget particle 1)])
(defn get-prev-pos [particle] [(aget particle 2) (aget particle 3)])
(defn get-acc [particle] [(aget particle 4) (aget particle 5)])

(defn set-pos! [particle x y] (aset particle 0 x) (aset particle 1 y))
(defn set-prev-pos! [particle x y] (aset particle 2 x) (aset particle 3 y))
(defn set-acc! [particle x y] (aset particle 4 x) (aset particle 5 y))

(defn apply-force
  [particle [fx fy]]
  (let [[ax ay] (get-acc particle)] (set-acc! particle (+ ax fx) (+ ay fy)))
  particle)


(defn accelerate-particle
  [particle dt]
  (let [dt-sq (* dt dt)
        [px py] (get-prev-pos particle)
        [ax ay] (get-acc particle)]
    (set-prev-pos! particle (+ px (* ax dt-sq)) (+ py (* ay dt-sq)))
    (set-acc! particle 0 0))
  particle)

(defn update-particle
  [particle]
  (let [[x y] (get-pos particle)
        [px py] (get-prev-pos particle)]
    (set-pos! particle (- (* 2 x) px) (- (* 2 y) py))
    (set-prev-pos! particle x y)))

(defn draw-particles
  [state]
  (let [buffer (:buffer @state)
        positions (js/Float32Array. buffer)
        num-particles (:num-particles @state)
        ctx (:ctx @state)
        width (.-width (.-canvas ctx))
        height (.-height (.-canvas ctx))
        radius (:radius @state)]
    (.clearRect ctx 0 0 width height)
    (dotimes [i num-particles]
      (let [x (aget positions (offset i))
            y (aget positions (inc (offset i)))]
        (.beginPath ctx)
        (.arc ctx x y radius 0 (* 2 Math/PI) false)
        (.fill ctx)))))

(defn update-all
  [state dt]
  (let [buffer (:buffer @state)
        num-particles (:num-particles @state)
        gravity (:gravity @state)]
    (dotimes [i num-particles]
      (let [particle (js/Float32Array. buffer (byte-offset i) (:size-p @state))]
        (-> particle
            (apply-force gravity)
            (accelerate-particle dt)
            (update-particle))))))

(defn bounce-all
  [state]
  (let [buffer (:buffer @state)
        num-particles (:num-particles @state)
        radius (:radius @state)
        width (:width @state)
        height (:height @state)]
    (dotimes [i num-particles]
      (let [particle (js/Float32Array. buffer (byte-offset i) (:size-p @state))
            [x y] (get-pos particle)
            [px py] (get-prev-pos particle)
            dx (- px x)
            dy (- py y)]
        (cond (< x radius) (aset particle 0 (+ x (* 2 dx)))
              (> x (- width radius)) (aset particle 0 (+ x (* 2 dx)))
              (< y radius) (aset particle 1 (+ y (* 2 dy)))
              (> y (- height radius)) (aset particle 1 (+ y (* 2 dy))))))))

(defn collide-all
  [state]
  (let [buffer (:buffer @state)
        num-particles (:num-particles @state)
        radius (:radius @state)
        min-dist (+ radius radius)]
    (dotimes [i num-particles]
      (let [particle-a
              (js/Float32Array. buffer (byte-offset i) (:size-p @state))
            [xa ya] (get-pos particle-a)]
        (loop [j 0]
          (when (and (not (= i j)) (<= j num-particles))
            (let [particle-b
                    (js/Float32Array. buffer (byte-offset j) (:size-p @state))
                  [xb yb] (get-pos particle-b)
                  vx (- xa xb)
                  vy (- ya yb)
                  dist-sq (+ (* vx vx) (* vy vy))]
              (when (< dist-sq (* min-dist min-dist))
                (let [dist (js/Math.sqrt dist-sq)
                      factor (* (/ (- dist min-dist) dist) 0.5)
                      dix (* vx factor)
                      diy (* vy factor)]
                  (set-pos! particle-a (- xa dix) (- ya diy))
                  (set-pos! particle-b (+ xb dix) (+ yb diy)))))
            (recur (inc j))))))))

(defn run
  []
  (do (update-all state (/ (:dt @state) 1000))
      (collide-all state)
      (bounce-all state)
      (draw-particles state)))
