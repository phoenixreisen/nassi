(ns nassi.transform
  (:require 
    [nassi.parse :as p]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.walk :as w]
    [editscript.core :as e]
    [hiccup.core :as h]
    [hiccup2.core :as h2]
    [instaparse.core :as insta]
    [markdown.core :as md]
    [nassi.md-para :as md-para]
    [nassi.util :as u]
    [nassi.steps :as steps]))

(def ^:dynamic *gen-options* 
  {:true "yes"
   :false "no"
   :catch "Exception-Handling"})

(def ^:private ^:const BLACK-RIGHT-POINTING-TRIANGLE "&#x25B6")

(def ^:private ^:const BLACK-LEFT-POINTING-TRIANGLE "&#x25C0")  

(defn- xf-diagram [& xs] [:div.diagram xs])

(defn- xf-block [& xs] xs)

(defn- xf-for [[step text] block] 
  [:div.for 
   [:div.expression step
    [:div.expression-text text]]
   [:div.statement block]])

(defn- xf-while [[step text] block] 
  [:div.while 
   [:div.expression step
    [:div.expression-text text]]
   [:div.statement block]])

(defn- xf-until [[step text] block] 
  [:div.until 
   [:div.expression step
    [:div.expression-text text]]
   [:div.statement block]])

(defn- xf-switch [[step text] cases] 
  [:div.branching
   [:div.expression step
    [:div.expression-text text]]
   [:div.branches
    cases]])

(defn- xf-cases [ & cases] cases)

(defn- xf-case [[step text] block] 
 [:div.branch 
  [:div.expression step
   [:div.expression-text text]]
  [:div.statement block]]) 

(defn- xf-default [[step text] block] 
 [:div.default-branch 
  [:div.expression step
   [:div.expression-text text]]
  [:div.statement block]]) 

(defn- xf-textstmt [[step text]]
  [:div.block step [:br] text])

(defn- process-internal-links 
  "Internal links have the form `[#id-of-something](#id-of-something)`. 
  This FN replaces the link-text of each internal link with its corresponding
  step number."
  [id->step s]
  (reduce (fn [ret [id step]]
            (str/replace ret (str "[" id "]") (str "[" step "]"))) 
    s id->step))

(defn- xf-text [id->step id st s] 
  [[:div.step {:id (when id (subs id 2))} st] ; remove !! from start of id
   (md/md-to-html-string 
     (process-internal-links id->step 
       (md-para/trim-paragraph s)))])

(defn- xf-else [block]
  [:div.default-branch 
   [:div.expression 
    [:div.expression-text [:b (get *gen-options* :false)]]]
   [:div.statement block]])

(defn- xf-if 
  ([[step text] block] 
   [:div.branching
    [:div.expression step
     [:div.expression-text text]]
    [:div.branches
     [:div.branch 
      [:div.expression 
       [:div.expression-text [:b (get *gen-options* :true)]]]
      [:div.statement block]]
     [:div.default-branch
      [:div.expression
       [:div.expression-text [:b (get *gen-options* :false)]]]
      [:div.statement]]]])
  ([[step text] block else] 
   [:div.branching
    [:div.expression step
     [:div.expression-text text]]
    [:div.branches
     [:div.branch 
      [:div.expression 
       [:div.expression-text [:b (get *gen-options* :true)]]]
      [:div.statement block]]
     else]]))

(defn- xf-throw [[_ error-code] [step text]] 
  [:div.block step 
   [:b BLACK-LEFT-POINTING-TRIANGLE " " (subs error-code 1)]
   text])

(defn- xf-catch [handlers] 
  (list 
    [:div.empty]
    [:div.branching.no-default-branch
     [:div.expression 
      [:div.expression-text [:b (get *gen-options* :catch)]]]
     [:div.branches
      handlers]]))

(defn- xf-handlers [ & handlers] handlers)

(defn- xf-errorcoderef [error-code step]
  [:div [:div.step step]
   [:b BLACK-RIGHT-POINTING-TRIANGLE " " (subs error-code 1)]])

(defn- xf-handle [x block] 
 [:div.branch 
  [:div.expression 
   [:div.expression-text x]]
  [:div.statement block]]) 

(defn to-html [x]
  (let [ast (steps/add-steps
              (p/parse-diagram x))
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
         :IF          xf-if
         :THROW       xf-throw
         :CATCH       xf-catch
         :HANDLERS    xf-handlers
         :HANDLE      xf-handle
         :ERRORCODEREF xf-errorcoderef
         } 
        ast))))

; -------------------------------------------------------------
