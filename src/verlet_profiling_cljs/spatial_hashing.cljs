(ns verlet-profiling-cljs.spatial-hashing
  (:require [verlet-profiling-cljs.state :refer [state]]
            [verlet-profiling-cljs.utils :refer [rand-range add-particle]]))

(defn- offset [i] (* i (:size-p @state)))

(defn- get-pos
  [particles idx]
  [(aget particles (+ idx 0)) (aget particles (+ idx 1))])
(defn- get-prev-pos
  [particles idx]
  [(aget particles (+ idx 2)) (aget particles (+ idx 3))])
(defn- get-acc
  [particles idx]
  [(aget particles (+ idx 4)) (aget particles (+ idx 5))])

(defn- set-pos!
  [particles idx x y]
  (aset particles (+ idx 0) x)
  (aset particles (+ idx 1) y))
(defn- set-prev-pos!
  [particles idx x y]
  (aset particles (+ idx 2) x)
  (aset particles (+ idx 3) y))
(defn- set-acc!
  [particles idx x y]
  (aset particles (+ idx 4) x)
  (aset particles (+ idx 5) y))

(defn- apply-force
  [particles idx [fx fy]]
  (let [[ax ay] (get-acc particles idx)]
    (set-acc! particles idx (+ ax fx) (+ ay fy)))
  particles)

(defn- accelerate-particle
  [particles idx dt]
  (let [dt-sq (* dt dt)
        [px py] (get-prev-pos particles idx)
        [ax ay] (get-acc particles idx)]
    (set-prev-pos! particles idx (+ px (* ax dt-sq)) (+ py (* ay dt-sq)))
    (set-acc! particles idx 0 0))
  particles)

(defn- update-particle
  [particles idx]
  (let [[x y] (get-pos particles idx)
        [px py] (get-prev-pos particles idx)]
    (set-pos! particles idx (- (* 2 x) px) (- (* 2 y) py))
    (set-prev-pos! particles idx x y)))

(defn clear-screen
  [state]
  (let [ctx (:ctx @state)
        width (.-width (.-canvas ctx))
        height (.-height (.-canvas ctx))]
    (.clearRect ctx 0 0 width height)))

(defn- draw-particles
  [state]
  (let [ctx (:ctx @state)
        radius (:radius @state)
        num-particles (:num-particles @state)
        particles (:particles @state)]
    (dotimes [i num-particles]
      (let [[x y] (get-pos particles (offset i))]
        (.beginPath ctx)
        (.arc ctx x y radius 0 (* 2 Math/PI) false)
        (.fill ctx)))))

(defn- update-all
  [state dt]
  (let [particles (:particles @state)
        num-particles (:num-particles @state)
        gravity (:gravity @state)]
    (dotimes [i num-particles]
      (-> particles
          (apply-force (offset i) gravity)
          (accelerate-particle (offset i) dt)
          (update-particle (offset i))))))

(defn- bounce-all
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

(defn get-hash-key
  [x y]
  (str (.floor js/Math (/ x (* 2 (:radius @state))))
       ","
       (.floor js/Math (/ y (* 2 (:radius @state))))))

(defn create-spatial-hash
  [state]
  (let [hash (atom {})
        particles (:particles @state)
        radius (:radius @state)
        num-particles (:num-particles @state)
        d (* 2 radius)]
    (dotimes [i num-particles]
      (let [idx (offset i)
            [x y] (get-pos particles idx)]
        (doseq [xoff [(- d) 0 d]
                yoff [(- d) 0 d]
                :let [key (get-hash-key (+ x xoff) (+ y yoff))]]
          (swap! hash update-in [key] (fnil conj []) i))))
    (swap! state assoc :spatial-hash hash)
    hash))

(defn draw-hash-key
  ([ctx key radius] (draw-hash-key ctx key radius "black"))
  ([ctx key radius color]
   (let [[x y] (map #(js/parseInt %) (.split key ","))]
     (set! (.-strokeStyle ctx) color)
     (.beginPath ctx)
     (.rect ctx (* x (* 2 radius)) (* y (* 2 radius)) (* 2 radius) (* 2 radius))
     (.stroke ctx))))

(defn draw-hash
  [state]
  (let [ctx (:ctx @state)
        radius (:radius @state)
        hash (:spatial-hash @state)]
    (doseq [[key] @hash] (draw-hash-key ctx key radius))))



(defn- collide-all
  [state]
  (dotimes [i (:num-particles @state)]
    (let [idxa (offset i)
          particles (:particles @state)
          [xa ya] (get-pos particles idxa)
          key (get-hash-key xa ya)
          hash (:spatial-hash @state)
          radius (:radius @state)
          min-dist (+ radius radius)]
      (doseq [j (get @hash key)]
        (when-not (= i j)
          (let [idxb (offset j)
                [xb yb] (get-pos particles idxb)
                vx (- xa xb)
                vy (- ya yb)
                dist-sq (+ (* vx vx) (* vy vy))]
            (when (< dist-sq (* min-dist min-dist))
              #_(draw-hash-key (:ctx @state) key radius "red")
              (let [dist (js/Math.sqrt dist-sq)
                    factor (* (/ (- dist min-dist) dist) 0.5)
                    dix (* vx factor)
                    diy (* vy factor)]
                (set-pos! particles idxa (- xa dix) (- ya diy))
                (set-pos! particles idxb (+ xb dix) (+ yb diy))))))))))

(defn draw-grid
  [state]
  (let [ctx (:ctx @state)
        width (.-width (.-canvas ctx))
        height (.-height (.-canvas ctx))
        cellsize (* 2 (:radius @state))
        num-x (.ceil js/Math (/ width cellsize))
        num-y (.ceil js/Math (/ height cellsize))]
    (dotimes [i num-x]
      (.beginPath ctx)
      (.moveTo ctx (* i cellsize) 0)
      (.lineTo ctx (* i cellsize) height)
      (.stroke ctx))
    (dotimes [i num-y]
      (.beginPath ctx)
      (.moveTo ctx 0 (* i cellsize))
      (.lineTo ctx width (* i cellsize))
      (.stroke ctx))))


(defn init
  [state]
  (print "init spatial-hashing")
  (let [ctx (:ctx @state)
        radius (:radius @state)
        num-particles (:num-particles @state)
        width (.-width (.-canvas ctx))
        height (.-height (.-canvas ctx))
        size-p (:size-p @state)]
    (swap! state assoc :particles (js/Float32Array. (* num-particles size-p)))
    (swap! state assoc :spatial-hash {})
    (dotimes [i num-particles]
      (add-particle state
                    (rand-range radius (- width radius))
                    (rand-range radius (- height radius))
                    (* i size-p)))))

(defn run
  [dt]
  (clear-screen state)
  (create-spatial-hash state)
  #_(draw-hash state)
  (update-all state dt)
  (collide-all state)
  (bounce-all state)
  (draw-particles state))
