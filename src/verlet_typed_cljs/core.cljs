(ns verlet-typed-cljs.core
  (:require [verlet-typed-cljs.state :refer [state]]
            [verlet-typed-cljs.utils :refer [init-base!]]
            [verlet-typed-cljs.still-slow :as still-slow]
            [verlet-typed-cljs.slower :as slower]
            [verlet-typed-cljs.slow :as slow]
            [verlet-typed-cljs.naive :as naive]
            ["react" :as react]
            [reagent.ratom :as ratom]
            [reagent.dom :as d]))

(def modes
  {:still-slow {:init still-slow/init, :run still-slow/run},
   :slow {:init slow/init, :run slow/run},
   :slower {:init slower/init, :run slower/run},
   :naive {:init naive/init, :run naive/run}})

(defn use-raf
  "Takes a function to pass to RAF and returns the animate function
  and a function to cancel the RAF ID."
  [step-fn & args]
  (let [raf (atom nil)
        pt (atom 0)
        animate
          (fn animate* [t]
            (when-not (nil? t) (step-fn (/ (- t @pt) 100) args) (reset! pt t))
            (reset! raf (.requestAnimationFrame js/window animate*)))
        cancel (fn []
                 (print "Cacelling" @raf)
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
        num-particles (ratom/make-reaction #(get @state :num-particles))
        ; prev-time (r/atom 0)
       ]
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
         :width 900,
         :height 900}]])))

(defn mode-selector
  []
  [selector
   {:options ["still-slow" "slow" "slower" "naive"],
    :default-value "still-slow",
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
     :max 300,
     :default-value 50,
     :on-change #(swap! state assoc
                   :radius
                   (/ (js/parseInt (.-value (.-target %))) 10))}]])

(print [mode-selector])

(defn app
  []
  [:div [mode-selector] [num-particles-selector] [radius-selector] [:f> canvas]
   [:div [debug-pre state]]])

(defn mount-root [app] (d/render [app] (.getElementById js/document "app")))

; (defn on-js-reload [] (mount-root app))

(mount-root app)
