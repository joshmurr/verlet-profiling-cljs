(defproject verlet-typed-cljs "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0",
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.11.132"]
                 [org.clojure/core.async "1.6.681"]
                 [reagent "1.2.0"]
                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]]
  :plugins [[lein-figwheel "0.5.20"]
            [lein-cljsbuild "1.1.8" :exclusions [[org.clojure/clojure]]]]
  :main ^:skip-aot verlet-typed-cljs.core
  :target-path "target/%s"
  :clean-targets
    ^{:protect false}
    ["resources/public/js/out" "resources/public/js/app.js" :target-path]
  :source-paths ["src"]
  :cljsbuild
    {:builds [{:id "dev",
               :source-paths ["src"],
               :figwheel {:on-jsload "verlet-typed-cljs.core/on-js-reload",
                          :open-urls ["http://localhost:3449"]},
               :compiler {:main "verlet-typed-cljs.core",
                          :asset-path "js/out",
                          :output-to "resources/public/js/app.js",
                          :output-dir "resources/public/js/out",
                          :optimizations :none}}
              {:id "release",
               :source-paths ["src"],
               :compiler {:main "verlet-typed-cljs.core",
                          :asset-path "js/out",
                          :output-to "resources/public/release/js/app.js",
                          :output-dir "resources/public/release/js/out",
                          :source-map "resources/public/release/js/app.js.map",
                          :optimizations :advanced}}]}
  :profiles {:dev {:dependencies [[binaryage/devtools "1.0.0"]
                                  [figwheel-sidecar "0.5.20"]],
                   ;; need to add dev source path here to get user.clj
                   ;; loaded
                   :source-paths ["src"],
                   ;; need to add the compiled assets to the :clean-targets
                   :clean-targets ^{:protect false}
                                  ["resources/public/js/compiled"
                                   :target-path]}})
