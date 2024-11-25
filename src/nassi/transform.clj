;; TODO rename NS to html
(ns nassi.transform
  "In this NS contains the functionality to transform an AST into a
  HTML-Document."
  (:require 
    [nassi.parse :as p]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [hiccup.core :as h]
    [instaparse.core :as insta]
    [markdown.core :as md]
    [nassi.md-para :as md-para]
    [nassi.steps :as steps]))

(def ^:dynamic *gen-options* 
  {:true "yes"
   :false "no"
   :catch "Exception-Handling"
   :diff-change-bg "yellow"
   :diff-insert-bg "lightgreen"
   :diff-delete-bg "lightpink"})

(defn- style [{:keys [diff-style]}]
  (when-some [key (get {:style-diff-delete        :diff-delete-bg
                        :style-diff-insert        :diff-insert-bg
                        :style-diff-change-delete :diff-change-bg
                        :style-diff-change        :diff-change-bg}
                    diff-style)]
    (cond-> (str "background-color: " (get *gen-options* key))
      (#{:style-diff-delete :style-diff-change-delete} diff-style)
      (str "; text-decoration:line-through"))))

(def ^:private ^:const BLACK-RIGHT-POINTING-TRIANGLE "&#x25B6")

(def ^:private ^:const BLACK-LEFT-POINTING-TRIANGLE "&#x25C0")  

(defn- xf-diagram [ctx & xs] [:div.diagram xs])

(defn- xf-block [ctx & xs] xs)

(defn- xf-sub [ctx [step text] block] 
  ;; TODO FIXME when we have CSS class for sub
  [:div.while {:style (style ctx)}
   [:div.expression step
    [:div.expression-text text]]
   [:div.statement block]])

(defn- xf-for [ctx [step text] block] 
  [:div.for {:style (style ctx)}
   [:div.expression step
    [:div.expression-text text]]
   [:div.statement block]])

(defn- xf-while [ctx [step text] block] 
  [:div.while {:style (style ctx)}
   [:div.expression step
    [:div.expression-text text]]
   [:div.statement block]])

(defn- xf-until [ctx [step text] block] 
  [:div.until {:style (style ctx)}
   [:div.expression step
    [:div.expression-text text]]
   [:div.statement block]])

(defn- xf-switch [ctx [step text] cases] 
  [:div.branching {:style (style ctx)}
   [:div.expression step
    [:div.expression-text text]]
   [:div.branches
    cases]])

(defn- xf-cases [ctx & xs] xs)

(defn- xf-case [ctx [step text] block] 
 [:div.branch {:style (style ctx)}
  [:div.expression step
   [:div.expression-text text]]
  [:div.statement block]]) 

(defn- xf-default [ctx [step text] block] 
 [:div.default-branch {:style (style ctx)}
  [:div.expression step
   [:div.expression-text text]]
  [:div.statement block]]) 

(defn- xf-textstmt [ctx [step text]]
  [:div.block step [:br] text])

(defn- xf-text [{:keys [id step] :as ctx} s] 
  [[:div.step {:id id} step]
   [:div {:style (style ctx)}
    (md/md-to-html-string (md-para/trim-paragraph s))]])

(defn- xf-else [ctx block]
  [:div.default-branch {:style (style ctx)}
   [:div.expression 
    [:div.expression-text [:b (get *gen-options* :false)]]]
   [:div.statement block]])

(defn- xf-if 
  ([ctx [step text] block] 
   [:div.branching
    [:div.expression step
     [:div.expression-text text]]
    [:div.branches
     [:div.branch {:style (style ctx)}
      [:div.expression 
       [:div.expression-text [:b (get *gen-options* :true)]]]
      [:div.statement block]]
     [:div.default-branch
      [:div.expression
       [:div.expression-text [:b (get *gen-options* :false)]]]
      [:div.statement]]]])
  ([ctx [step text] block else] 
   [:div.branching
    [:div.expression step
     [:div.expression-text text]]
    [:div.branches
     [:div.branch {:style (style ctx)}
      [:div.expression 
       [:div.expression-text [:b (get *gen-options* :true)]]]
      [:div.statement block]]
     else]]))

(defn- xf-throw [ctx [_ _ error-code] [step text]] 
  [:div.block step 
   [:b BLACK-LEFT-POINTING-TRIANGLE " " (subs error-code 1)]
   text])

(defn- xf-catch [ctx handlers] 
  (list 
    [:div.empty]
    [:div.branching.no-default-branch {:style (style ctx)}
     [:div.expression 
      [:div.expression-text [:b (get *gen-options* :catch)]]]
     [:div.branches
      handlers]]))

(defn- xf-handlers [ctx & xs] xs)

(defn- xf-errorcoderef [{:keys [step]} error-code]
  [:div [:div.step step]
   [:b BLACK-RIGHT-POINTING-TRIANGLE " " (subs error-code 1)]])

(defn- xf-handle [ctx x block] 
 [:div.branch {:style (style ctx)}
  [:div.expression 
   [:div.expression-text x]]
  [:div.statement block]]) 

(defn- html-with-inline-css [html-body]
  (str/join \newline
    ["<!DOCTYPE html>" "<html>" "<head>" "<style>" 
     (slurp (io/resource "diagram.css"))
     "</style>" "</head>" "<body>" html-body "</body>" "</html>"]))

(defn- to-html-body 
  "Transforms an `ast` (obtained by `nassi.parse/parse-diagram`) into a HTML
  representation."
  [ast]
  (h/html 
    (insta/transform 
      {:DIAGRAM     xf-diagram
       :TEXTSTMT    xf-textstmt
       :TEXT        xf-text
       :BLOCK       xf-block
       :SUB         xf-sub
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
       :ERRORCODEREF xf-errorcoderef} 
      (steps/add-steps ast))))

(defn to-html [ast]
  (-> (to-html-body ast)
      (html-with-inline-css)))
