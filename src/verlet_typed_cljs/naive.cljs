(ns verlet-typed-cljs.naive
  (:require [verlet-typed-cljs.state :refer [state]]
            [verlet-typed-cljs.utils :refer [jiggle rand-range]]))

(defprotocol IParticle
  (apply-force [p f])
  (accelerate [p dt])
  (update-pos [p])
  (draw [p ctx])
  (bounce [p b])
  (collide-with [p op]))

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
        (->Particle x_ y_ px py ax ay)))
    (collide-with [p op]
      (let [dx (- (:x op) x)
            dy (- (:y op) y)
            d2 (+ (* dx dx) (* dy dy))
            r2 (* 4 (:radius @state) (:radius @state))
            min-dist (* 2 (:radius @state))]
        (if (< d2 r2)
          (let [d (Math/sqrt d2)
                factor (* (/ (- d min-dist) d) 0.5)
                x_ (- x (* dx factor))
                y_ (- y (* dy factor))]
            (->Particle x_ y_ px py ax ay))
          p))))

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

(defn collide-all
  [particles]
  (let [idx-arr (zipmap (range) particles)]
    (map (fn [[i p1]]
           (reduce (fn [prevp [j nextp]]
                     (if (= i j) prevp (collide-with prevp nextp)))
             p1
             idx-arr))
      idx-arr)))

(defn run
  []
  (.clearRect (:ctx @state) 0 0 900 900)
  (swap! state update :particles (partial map update-all))
  (swap! state update :particles (partial map #(bounce % [900 900])))
  (swap! state update :particles collide-all))
