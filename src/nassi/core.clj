(ns nassi.core
  (:import 
    (org.commonmark.node Node)
    (org.commonmark.parser Parser)
    (org.commonmark.renderer.html HtmlRenderer))
  (:require [clojure.string :as str]
            [nassi.steps :as steps]
            [editscript.core :as e]
            [instaparse.core :as insta]
            [hiccup.core :as h]
            [markdown.core :as md]
            [clojure.java.io :as io]
            [clojure.walk :as w]))

(def ^:private parse 
  "This FN accepts a textual representation of a nassi-shneiderman diagram and
  returns either an abstract syntax tree, or a failure."
  (insta/parser (io/resource "grammar.bnf") :string-ci true))

(defn parse-diagram [x]
  (parse (slurp x)))

(defn- xf-diagram [& xs] [:div.diagram xs])
(defn- xf-block [& xs] [:div.block xs])
(defn- xf-for [s b] [:div.for s b])
(defn- xf-while [s b] [:div.while s b])
(defn- xf-until [s b] [:div.until s b])
(defn- xf-switch [exp cases] [:div.switch exp cases])
(defn- xf-cases [ & cases] cases)
(defn- xf-case [exp block] [:div exp block])

(defn- xf-text [id->step id st s] 
  [:div.block 
   {:id (when id (subs id 1))} ; remove # from start 
   [:b st] ": " (md/md-to-html-string s)])

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
  (let [ast (steps/add-steps
              (parse-diagram x))
        id->step (steps/id->step ast)]
    (h/html 
      (insta/transform 
        {:DIAGRAM     xf-diagram
         :PARAGRAPH   (partial xf-text id->step)
         :SENTENCE    (partial xf-text id->step)
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

#_(spit "/home/jan/repos/phoenixreisen/dfb/doc/UC-001_Vorlage-erstellen.html"
  (str "<html> <head> <link rel=\"stylesheet\" href=\"diagram.css\"> </head><body>"
    (to-html "/home/jan/repos/phoenixreisen/dfb/doc/UC-001_Vorlage-erstellen.uc") "</body> </html>"))

#_(spit "/home/jan/tmp/uc.html"
  (str "<html> <head> <link rel=\"stylesheet\" href=\"diagram.css\"> </head><body>"
    (to-html (io/resource "GutenMorgen2.uc")) "</body> </html>"))

;(def a (parse (slurp (io/resource "ex0.uc"))))
;(def b (parse (slurp (io/resource "ex1.uc"))))
;(e/diff a b)

; -------------------------------------------------------------

;; - Warnen, wenn die gleiche ID in verschiedenen SENTENCE bzw. PARAGRAPH
;;   Knoten verwendet wird
;; - Warnen, wenn eine Exception nicht behandelt wird.
;; - Ueberlegen, wie man Schritte referenziert

(comment
(parse-diagram (io/resource "GutenMorgen2.uc"))

  ;; Damit Links innerhalb eins Dokuments funktionieren muessen wir alle
  ;; "[#id](#id)" Vorkommnisse durch "[step](#id)" austauschen.

  (require '[markdown.core :as md])
  (def input       "# This is a test\n <a name=\"abcde\">some<a/> code follows\n```clojure\n(defn foo [])\n```Let's link back to [AbCdE](abcde) ")
  (def input "hello [github](http://github.com)")
  (def input "hello [github](#x1)")
  (def input "hello [github](#x1)")
  (def parser (.build (Parser/builder)))
  (def document (.parse parser input))
  (def renderer (.build (HtmlRenderer/builder)))
  (.render renderer document);  // "<p>This is <em>Markdown</em></p>\n"
  (println (md/md-to-html-string input))


  (println (md/md-to-html-string "hello [github](#x1)"))

  )
