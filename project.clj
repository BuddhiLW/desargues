(defproject desargues "0.1.0-SNAPSHOT"
  :description "Mathematical animation DSL bridging Emmy and Manim"
  :url "https://github.com/yourusername/desargues"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.3"]
                 [org.mentat/emmy "0.32.0"]
                 [clj-python/libpython-clj "2.026"]
                 ;; Hot reload infrastructure
                 [com.nextjournal/beholder "1.0.2"]
                 [io.github.tonsky/clj-reload "0.7.1"]
                 ;; Type safety
                 [org.clojure/test.check "1.1.1"]
                 [org.typedclojure/typed.clj.checker "1.3.0"]
                 [org.typedclojure/typed.clj.runtime "1.3.0"]]
  :main ^:skip-aot desargues.core
  :target-path "target/%s"
  :source-paths ["src"]
  :test-paths ["test"]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[orchestra "2021.01.01-1"]]}
             :test {:jvm-opts ["-Dclojure.spec.check-asserts=true"]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"typecheck" ["run" "-m" "typed.clj.checker.check-ns"
                         "desargues.domain.protocols"
                         "desargues.domain.math-expression"
                         "desargues.domain.services"]
            "test-all" ["do" ["typecheck"] ["test"]]
            "test-props" ["test" ":only" "desargues.properties"]})
