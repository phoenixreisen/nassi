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

(defn- xf-block [& xs] xs)

(defn- xf-for [exp block] 
  [:div.for 
   [:div.expression 
    [:div.expression-text exp]]
   [:div.statement block]])

(defn- xf-while [exp block] 
  [:div.while 
   [:div.expression 
    [:div.expression-text exp]]
   [:div.statement block]])

(defn- xf-until [exp block] 
  [:div.until 
   [:div.expression 
    [:div.expression-text exp]]
   [:div.statement block]])

(defn- xf-switch [exp cases] 
  [:div.branching 
   [:div.expression 
    [:div.expression-text exp]]
   cases])

(defn- xf-cases [ & cases] cases)

(defn- xf-case [exp block] 
 [:div.branch 
  [:div.expression 
   [:div.expression-text exp]]
  [:div.statement block]]) 

(defn- xf-default [exp block] 
 [:div.default-branch 
  [:div.expression 
   [:div.expression-text exp]]
  [:div.statement block]]) 

(defn- xf-textstmt [x]
  [:div.block x])

(defn- process-internal-links 
  "Internal links have the form `[#id-of-something](#id-of-something)`. 
  This FN replaces the link-text of each internal link with its corresponding
  step number."
  [id->step s]
  (reduce (fn [ret [id step]]
            (str/replace ret (str "[" id "]") (str "[" step "]"))) 
    s id->step))

(defn- xf-text [id->step id st s] 
  ;; TODO das aeussere DIV brauchen wir momentan nur als eventuelles Ziel fuer
  ;; Links. Kann man dann spaeter in das DIV verlagern, das den Step anzeigt.
  [:div {:id (when id (subs id 1))} ; remove # from start of id
   (md/md-to-html-string 
     (str "<b>" st ":</b>" 
       (process-internal-links id->step s)))])

(defn- xf-else [block]
  [:div.default-branch 
   [:div.expression 
    [:div.expression-text "false"]]
   [:div.statement block]])

(defn- xf-if 
  ([exp block] 
   [:div.branching 
    [:div.expression 
     [:div.expression-text exp]]
    [:div.branch 
     [:div.expression 
      [:div.expression-text "true"]]
     [:div.statement block]]])
  ([exp block else] 
   [:div.branching 
    [:div.expression 
     [:div.expression-text exp]]
    [:div.branch 
     [:div.expression 
      [:div.expression-text "true"]]
     [:div.statement block]]
    else]))

(defn to-html [x]
  (let [ast (steps/add-steps
              (parse-diagram x))
        id->step (steps/id->step ast)]
    (h/html 
      (insta/transform 
        {:DIAGRAM     xf-diagram
         :TEXTSTMT    xf-textstmt
         :PARAGRAPH   (partial xf-text id->step)
         :SENTENCE    (partial xf-text id->step)
         :BLOCK       xf-block
         :FOR         xf-for
         :WHILE       xf-while
         :UNTIL       xf-until
         :SWITCH      xf-switch
         :CASES       xf-cases
         :CASE        xf-case
         :DEFAULT     xf-default
         :ELSE        xf-else
         :IF          xf-if} 
        ast))))

#_(spit "/home/jan/repos/phoenixreisen/dfb/doc/UC-001_Vorlage-erstellen.html"
  (str "<html> <head> <link rel=\"stylesheet\" href=\"diagram.css\"> </head><body>"
    (to-html "/home/jan/repos/phoenixreisen/dfb/doc/UC-001_Vorlage-erstellen.uc") "</body> </html>"))

#_(spit "/home/jan/tmp/uc.html"
  (str "<html> <head> <link rel=\"stylesheet\" href=\"diagram.css\"> </head><body>"
    (to-html (io/resource "GutenMorgen2.uc")) "</body> </html>"))

#_(spit "/home/jan/tmp/uc.html"
  (str "<html> <head> <link rel=\"stylesheet\" href=\"diagram.css\"> </head><body>"
    (to-html (io/resource "test/if2.uc")) "</body> </html>"))

;(def a (parse (slurp (io/resource "ex0.uc"))))
;(def b (parse (slurp (io/resource "ex1.uc"))))
;(e/diff a b)

; -------------------------------------------------------------

;; - Warnen, wenn eine Exception nicht behandelt wird.

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
