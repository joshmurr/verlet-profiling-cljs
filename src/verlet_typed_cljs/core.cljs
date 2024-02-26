(ns verlet-typed-cljs.core
  (:require [verlet-typed-cljs.state :refer [state]]
            [verlet-typed-cljs.utils :refer [init-particles! mean conj-max]]
            [verlet-typed-cljs.still-slow :refer [run] :rename {run still-slow}]
            [verlet-typed-cljs.slower :refer [run] :rename {run slower}]
            [verlet-typed-cljs.slow :refer [run] :rename {run slow}]
            [reagent.core :as r]
            ["react" :as react]
            [reagent.ratom :as ratom]
            [reagent.dom :as d]))

(def modes {:still-slow still-slow, :slow slow, :slower slower})

(defn use-raf
  "Takes a function to pass to RAF and returns the animate function
  and a function to cancel the RAF ID."
  [step-fn & args]
  (let [raf (atom nil)
        animate (fn animate* [t]
                  (step-fn t args)
                  (reset! raf (.requestAnimationFrame js/window animate*)))
        cancel (fn []
                 (print "Cacelling" @raf)
                 (.cancelAnimationFrame js/window @raf))]
    [animate cancel]))

(defn moving-avg-comp
  []
  (let [fps (ratom/make-reaction #(get @state :moving-avg))]
    (fn [] [:pre "fps: " (.floor js/Math (/ 1000 @fps))])))

(defn mode-selector
  []
  (fn [] [:select
          {:default-value "none",
           :on-change #(swap! state assoc :mode (.-value (.-target %)))}
          (for [m ["none" "still-slow" "slow" "slower"]]
            [:option {:key m, :value m} m])]))

(defn canvas
  []
  (let [ref (atom nil)
        mode (ratom/make-reaction #(get @state :mode))
        prev-time (r/atom 0)]
    (fn []
      (react/useEffect (fn []
                         (when (not (nil? @ref))
                           (swap! state assoc :ctx (.getContext @ref "2d"))
                           (init-particles! state)
                           (let [[run cancel] (use-raf #(->> %
                                                             (reset! prev-time)
                                                             ((:still-slow
                                                                modes))))]
                             (run)
                             cancel)))
                       (array @mode))
      [:<>
       [:pre
        (Math/floor (/ 1000
                       (- (.. js/document -timeline -currentTime) @prev-time)))]
       [:canvas
        {:id "particle-canvas",
         :ref #(reset! ref %),
         :width 900,
         :height 900}]])))

(defn app [] [:div [mode-selector] [:f> canvas]])

(defn mount-root [app] (d/render [app] (.getElementById js/document "app")))

; (defn on-js-reload [] (mount-root app))

(mount-root app)

(comment
  (defn cb
    [dt]
    (swap! state assoc
      :dt dt
      :moving-avg
        (mean (conj-max (:samples @state) (:moving-avg-window @state) dt)))))
