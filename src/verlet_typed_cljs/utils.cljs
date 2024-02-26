(ns verlet-typed-cljs.utils)

(defn rand-range [min max] (+ min (* (rand) (- max min))))
(defn jiggle [x] (+ x (* 2 (- (rand) 0.5))))
(defn mean [v] (/ (reduce + v) (count v)))
(defn pset!
  [arr idx els]
  (dotimes [i (count els)] (aset arr (+ i idx) (nth els i))))

(defn conj-max
  "Takes a vector, size limit and value x. Appends x to vector and
  ensures vector does not grow beyond limit."
  [vec limit x]
  (let [n (count vec)]
    (if (>= n limit) (conj (subvec vec (inc (- n limit))) x) (conj vec x))))

(defn add-particle
  [state x y idx]
  (let [particles (:particles @state)]
    (pset! particles idx [x y (jiggle x) (jiggle y)])))

(defn init-particles!
  [state]
  (let [ctx (:ctx @state)
        radius (:radius @state)
        num-particles (:num-particles @state)
        width (.-width (.-canvas ctx))
        height (.-height (.-canvas ctx))
        size-p (:size-p @state)]
    (swap! state assoc :particles (js/Float32Array. (* num-particles size-p)))
    (swap! state assoc :buffer (.-buffer (:particles @state)))
    (dotimes [i num-particles]
      (add-particle state
                    (rand-range radius (- width radius))
                    (rand-range radius (- height radius))
                    (* i size-p)))))

(defn with-timer
  [func t0 callbacks]
  (let [_ (func)
        dt (- (.now js/performance) t0)]
    (when callbacks (doseq [cb callbacks] (cb dt)))
    func))
