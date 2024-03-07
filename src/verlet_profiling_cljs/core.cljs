(ns verlet-profiling-cljs.core
  (:require
    [verlet-profiling-cljs.state :refer [state]]
    [verlet-profiling-cljs.utils :refer [init-base!]]
    [verlet-profiling-cljs.typed-array-helper-fns :as typed-array-helper-fns]
    [verlet-profiling-cljs.array-buffer :as array-buffer]
    [verlet-profiling-cljs.spatial-hashing :as spatial-hashing]
    [verlet-profiling-cljs.typed-array-inline-aget :as typed-array-inline-aget]
    [verlet-profiling-cljs.naive :as naive]
    ["react" :as react]
    [reagent.ratom :as ratom]
    [reagent.dom :as d]))

(def modes
  {:typed-array-helper-fns {:init typed-array-helper-fns/init,
                            :run typed-array-helper-fns/run},
   :typed-array-inline-aget {:init typed-array-inline-aget/init,
                             :run typed-array-inline-aget/run},
   :array-buffer {:init array-buffer/init, :run array-buffer/run},
   :naive {:init naive/init, :run naive/run},
   :spatial-hashing {:init spatial-hashing/init, :run spatial-hashing/run}})

(defn use-raf
  "Takes a function to pass to RAF and returns the animate function
  and a function to cancel the RAF ID."
  [step-fn & args]
  (let [raf (atom nil)
        pt (atom (.. js/document -timeline -currentTime))
        animate (fn animate* [t]
                  (when (and (some? @pt) (some? t))
                    (step-fn (/ (- t @pt) 1000) args))
                  (reset! pt t)
                  (reset! raf (.requestAnimationFrame js/window animate*)))
        cancel (fn []
                 (print "Cacelling" @raf)
                 (reset! pt 0)
                 (.cancelAnimationFrame js/window @raf))]
    [animate cancel]))

(defn selector
  [{:keys [options default-value on-change]}]
  [:select {:default-value default-value, :on-change on-change}
   (for [m options] [:option {:key m, :value m} m])])

(defn debug-pre
  [state]
  (let [running? (ratom/make-reaction #(get @state :running?))]
    (if running? [:pre (.stringify js/JSON (clj->js @state) nil 2)] nil)))

(defn canvas
  []
  (let [ref (atom nil)
        mode (ratom/make-reaction #(get @state :mode))
        num-particles (ratom/make-reaction #(get @state :num-particles))]
    (fn []
      (react/useEffect (fn []
                         (when-not (nil? @ref)
                           (let [{:keys [init run]} (get modes (:mode @state))
                                 [animate cancel] (use-raf run)]
                             (init-base! state @ref)
                             (init state)
                             (swap! state assoc :running? true)
                             (animate)
                             cancel)))
                       (array @mode @num-particles))
      [:div
       [:canvas
        {:id "particle-canvas",
         :ref #(reset! ref %),
         :width (:width @state),
         :height (:height @state)}]])))

(defn mode-selector
  []
  [selector
   {:options ["typed-array-helper-fns" "typed-array-inline-aget" "array-buffer"
              "naive" "spatial-hashing"],
    :default-value "typed-array-helper-fns",
    :on-change #(swap! state assoc :mode (keyword (.-value (.-target %))))}])

(defn num-particles-selector
  []
  [selector
   {:options ["3" "100" "300" "600" "1200"],
    :default-value "3",
    :on-change #(swap! state assoc
                  :num-particles
                  (js/parseInt (.-value (.-target %))))}])

(defn radius-selector
  []
  [:span [:label "Radius"]
   [:input
    {:type "range",
     :min 1,
     :max 600,
     :default-value 100,
     :on-change #(swap! state assoc
                   :radius
                   (/ (js/parseInt (.-value (.-target %))) 10))}]])

(defn app
  []
  [:div [mode-selector] [num-particles-selector] [radius-selector] [:f> canvas]
   #_[:div [debug-pre state]]])

(defn mount-root [app] (d/render [app] (.getElementById js/document "app")))

; (defn on-js-reload [] (mount-root app))

(mount-root app)
