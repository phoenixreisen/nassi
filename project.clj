(defproject nassi #=(clojure.string/trim #=(slurp "resources/VERSION"))
  :description "Generate beautiful nassi-shneiderman diagrams."
  :url "https://github.com/phoenixreisen/nassi"
  :license {:name "BSD-3-Clause"
            :url "https://opensource.org/license/bsd-3-clause"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.cli "1.1.230"]
                 [instaparse "1.5.0"] 
                 [hiccup "2.0.0-RC3"]
                 [markdown-clj "1.12.1"]
                 [org.commonmark/commonmark "0.22.0"]
                 [io.github.java-diff-utils/java-diff-utils "4.12"] ]

  :profiles {:uberjar {:aot :all}}
  :repl-options {:init-ns nassi.main}
  :uberjar-name "nassi.jar"
  :main nassi.main)
