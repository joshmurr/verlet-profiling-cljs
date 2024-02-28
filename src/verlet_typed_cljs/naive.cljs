(ns verlet-typed-cljs.naive
  (:require [verlet-typed-cljs.state :refer [state]]
            [verlet-typed-cljs.utils :refer [rand-range jiggle]]))

(defprotocol IParticle
  (x [p])
  (y [p])
  (px [p])
  (py [p])
  (ax [p])
  (ay [p])
  (apply-force [p f])
  (accelerate [p dt])
  (update-pos [p])
  (draw [p ctx])
  (bounce [p b])
  (collide [p op]))

(defrecord Particle [x y px py ax ay]
  IParticle
    (x [_] x)
    (y [_] y)
    (px [_] px)
    (py [_] py)
    (ax [_] ax)
    (ay [_] ay)
    (apply-force [_ [fx fy]] (->Particle x y px py (+ ax fx) (+ ay fy)))
    (accelerate [_ dt]
      (let [dt-sq (* dt dt)]
        (->Particle x y (+ px (* ax dt-sq)) (+ py (* ay dt-sq)) 0 0)))
    (update-pos [_] (->Particle (- (* x 2) px) (- (* y 2) py) x y 0 0))
    (draw [p ctx]
      (let [radius (:radius @state)]
        (.beginPath ctx)
        (.arc ctx x y radius 0 (* 2 Math/PI) false)
        (.fill ctx))
      p)
    (bounce [_ [w h]]
      (let [radius (:radius @state)
            dx (- px x)
            dy (- py y)
            x_ (if (or (< x radius) (> x (- w radius))) (+ x (* 2 dx)) x)
            y_ (if (or (< y radius) (> y (- h radius))) (+ y (* 2 dy)) y)]
        (->Particle x_ y_ px py ax ay))))

(defn new-particle [x y] (->Particle x y (jiggle x) (jiggle y) 0 0))

(defn init
  [state]
  (print "init naive")
  (let [radius (:radius @state)
        num-particles (:num-particles @state)
        width (:width @state)
        height (:height @state)
        particles (repeatedly num-particles
                              #(new-particle
                                 (rand-range radius (- width radius))
                                 (rand-range radius (- height radius))))]
    (swap! state assoc :particles particles)))

(defn update-all
  [p]
  (-> p
      (apply-force [0 -0.9])
      (accelerate 0.1)
      (update-pos)
      (draw (:ctx @state))))

(defn run
  []
  (swap! state update :particles (partial map update-all))
  (swap! state update :particles (partial map #(bounce % [900 900]))))
