(defproject nassi "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [instaparse "1.5.0"] 
                 [hiccup "2.0.0-RC3"]
                 [markdown-clj "1.12.1"]
                 [juji/editscript "0.6.3"]
                 [org.commonmark/commonmark "0.22.0"]
                 ]
  :repl-options {:init-ns nassi.core})
