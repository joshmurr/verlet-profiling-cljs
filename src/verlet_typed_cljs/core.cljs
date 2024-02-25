(ns verlet-typed-cljs.core
  (:require [verlet-typed-cljs.state :refer [state]]
            [verlet-typed-cljs.utils :refer [init-particles! with-timer mean]]
            [verlet-typed-cljs.still-slow :refer [run] :rename {run still-slow}]
            [verlet-typed-cljs.slower :refer [run] :rename {run slower}]
            [verlet-typed-cljs.slow :refer [run] :rename {run slow}]
            [reagent.core :as r]
            ["react" :as react]
            [reagent.ratom :as ratom]
            [reagent.dom :as d]))

(defonce run-fn (r/atom still-slow))

(def modes {:still-slow still-slow, :slow slow, :slower slower})

(defn conj-max
  "Takes a vector, size limit and value x. Appends x to vector and
  ensures vector does not grow beyond limit."
  [vec limit x]
  (let [n (count vec)]
    (if (>= n limit) (conj (subvec vec (inc (- n limit))) x) (conj vec x))))

(defn animate
  [step-fn & args]
  (fn animate* [t]
    (step-fn t args)
    (.requestAnimationFrame js/window animate*)))

(defn start
  [canvas run-fn]
  (let [cb #(swap! state assoc
              :dt %
              :moving-avg (mean (conj-max (:samples @state)
                                          (:moving-avg-window @state)
                                          %)))
        run- (animate (partial with-timer run-fn) cb)]
    (swap! state assoc :ctx (.getContext canvas "2d"))
    (init-particles! state)
    (run- 0)))

(defn moving-avg-comp
  []
  (let [fps (ratom/make-reaction #(get @state :moving-avg))]
    (fn [] [:pre "fps: " (.floor js/Math (/ 1000 @fps))])))

(defn mode-selector
  []
  (let [mode (ratom/make-reaction #(get @state :mode))]
    (fn [] [:select
            {:default-value @mode,
             :on-change #(swap! state assoc :mode (.-value (.-target %)))}
            (for [m ["still-slow" "slow" "slower"]]
              [:option {:key m, :value m} m])])))


(defn canvas
  [{:keys [width height mode]} props]
  (fn []
    (let [!canvas (atom nil)]
      (react/useEffect (fn [] (start @!canvas (get modes (keyword mode))))
                       js/undefined)
      [:canvas
       {:key mode,
        :id "particle-canvas",
        :ref #(reset! !canvas %),
        :width width,
        :height height}])))

(defn app
  []
  (let [mode (ratom/make-reaction #(get @state :mode))]
    (fn []
      (let [props
              {:width (:width @state), :height (:height @state), :mode @mode}]
        [:div [mode-selector] [moving-avg-comp] [:f> canvas props]]))))

(defn mount-root [app] (d/render [app] (.getElementById js/document "app")))

(defn on-js-reload [] (mount-root app))

(mount-root app)
