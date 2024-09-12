(ns nassi.core
  (:import 
    (org.commonmark.node Node)
    (org.commonmark.parser Parser)
    (org.commonmark.renderer.html HtmlRenderer))
  (:require [clojure.string :as str]
            [nassi.steps :as steps]
            [editscript.core :as e]
            [instaparse.core :as insta]
            [hiccup2.core :as h]
            [clojure.java.io :as io]
            [clojure.walk :as w]))

(defn- read-in 
  "Reads in a textual representation of a nassi-shneiderman diagram and
  returns a string that can be parsed with the `parse` FN."
  [x]
  (with-open [r (io/reader x)]
    (str/join \newline
      (->> (line-seq r)
           (remove str/blank?)
           (map str/trim)))))

(def ^:private parse 
  "This FN accepts a textual representation of a nassi-shneiderman diagram and
  returns either an abstract syntax tree, or a failure."
  (insta/parser (io/resource "grammar.bnf")))

(defn parse-diagram [x]
  (parse (read-in x)))

;(steps/add-steps (parse (read-in (io/resource "x0.uc"))))

(defn- xf-diagram [& xs] [:div.diagram xs])
(defn- xf-sentence [s] #_[:div s] [:div.block s])
(defn- xf-paragraph [s] #_[:div (str/replace s "\"\"\"" "")] [:div.block (str/replace s "\"\"\"" "")])
(defn- xf-block [& xs] [:div.block xs])
(defn- xf-for [s b] [:div.for s b])
(defn- xf-while [s b] [:div.while s b])
(defn- xf-until [s b] [:div.until s b])
(defn- xf-switch [exp cases] [:div.switch exp cases])
(defn- xf-cases [ & cases] cases)
(defn- xf-case [exp block] [:div exp block])

(defn- xf-if 
  ([s yes] 
   [:div.if [:div [:b s]]
    [:div 
     [:div [:b "true"]]
     [:div yes]]])
  ([s yes no] 
   [:div.if [:div [:b s]]
    [:div 
     [:div [:b "true"]]
     [:div yes]]
    [:div 
     [:div [:b "false"]]
     [:div no]]]))

(defn- to-html [x]
  (let [ast (parse-diagram (io/resource x))]
    (h/html 
      (insta/transform 
        {:DIAGRAM     xf-diagram
         :PARAGRAPH   xf-paragraph
         :SENTENCE    xf-sentence
         :BLOCK       xf-block
         :FOR         xf-for
         :WHILE       xf-while
         :UNTIL       xf-until
         :SWITCH      xf-switch
         :CASES       xf-cases
         :CASE        xf-case
         :DEFAULT     xf-case
         :IF          xf-if} 
        ast))))

#_(spit "/home/jan/tmp/uc.html"
  (str "<html> <head> <link rel=\"stylesheet\" href=\"diagram.css\"> </head><body>"
    (to-html  "test/GutenMorgen.uc") "</body> </html>"))

;(parse (read-in (io/resource "x2.uc")))
;(println (str (to-html "test/switch1.uc")))

;(def a (parse (read-in (io/resource "ex0.uc"))))
;(def b (parse (read-in (io/resource "ex1.uc"))))
;(e/diff a b)

; -------------------------------------------------------------

;; - Warnen, wenn zwei Exceptions mit der gleichen ID gefunden werden.
;; - Warnen, wenn eine Exception nicht behandelt wird.
;; - Ueberlegen, wie man Schritte referenziert
;; - if, then, else

(comment
  (require '[markdown.core :as md])
  (def input       "# This is a test\n <a name=\"abcde\">some<a/> code follows\n```clojure\n(defn foo [])\n```Let's link back to [AbCdE](abcde) ")
  (def input "hello [github](http://github.com)")
  (def parser (.build (Parser/builder)))
  (def document (.parse parser input))
  (def renderer (.build (HtmlRenderer/builder)))
  (.render renderer document);  // "<p>This is <em>Markdown</em></p>\n"
  (println 
    (md/md-to-html-string input)
      "# This is a test\n <a name=\"abcde\">some<a/> code follows\n```clojure\n(defn foo [])\n```Let's link back to [AbCdE](#abcde) ")
  )
