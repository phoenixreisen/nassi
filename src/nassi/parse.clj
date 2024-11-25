(ns nassi.parse
  (:require 
    [clojure.string :as str]
    [clojure.java.io :as io]
    [clojure.walk :as w]
    [instaparse.core :as insta]
    [nassi.util :as u]))

(def ^:private parse 
  "This FN accepts a textual representation of a nassi-shneiderman diagram and
  returns either an abstract syntax tree, or a failure."
  (insta/parser (io/resource "grammar.bnf") :string-ci true))

(def node-id-regex  
  "Matches double exclamation marks followed by a valid HTML id at the start of
  a string (ignoring leading whitespace in front of the id).

  Valid HTML id attribute values begin with a letter and must comprise only
  letters ( a - Z ), digits ( 0 - 9 ), hyphens ( - ), underscores ( _ ), and
  periods ( . ).
  
  Returns three groups, if this regex matches:
  
    - The leading whitespace before the id.
    - The id itself.
    - The text following the id.

  Ex.

  ```
  (re-matches node-id-regex \"\\t!!ID-1 Hello, World! \")
  => [\"\\t!!ID-1 Hello, World! \" \"\\t\" \"!!ID-1\" \" Hello, World! \"]
  ```" 
  #"(?s)(\s*)(!![A-Za-z][\.A-Za-z0-9_-]+)(.*)")

(declare parse-diagram)
(declare embed-diagrams)

(defn- embed-diagrams 
  "Embeds DIAGRAM nodes like so:

  ```
  (embed-diagrams [:DIAGRAM 1 2 3 [:DIAGRAM 4 5] [6 7 [8 9 [:DIAGRAM 10]]] 11 12])
  => [:DIAGRAM 1 2 3 4 5 [6 7 [8 9 10]] 11 12]
  ```"
  [xs] 
  (reduce (fn [ret x] 
            (cond 
              (= :DIAGRAM (u/node-type x)) (u/concatv ret (rest x))
              (vector? x) (conj ret (embed-diagrams x))
              :else (conj ret x)))
    [] xs))

(defn- preprocess-diagram [working-dir ast] 
  (embed-diagrams
    (w/prewalk 
      (fn [x] 
        (if (= :INCLUDE (u/node-type x))
          (let [[_ _ [_ _ fname]] x
                f (io/file working-dir (u/substr fname 1 -1))]
            (parse-diagram f))
          x)) 
      ast)))

(defn- add-node-contexts 
  "Each node gets a context map, with positional information that indicates
  which part of the source the node refers to and an ID that identifies the
  node uniquely."
  [ast]
  (w/postwalk
    (fn [x] 
      (cond
        (u/node? x) (let [[head & tail] x
                          {:instaparse.gll/keys [start-line end-line]} (meta x)
                          ctx {:uid (u/next-uid!)
                               :range {:start-line start-line 
                                       :end-line end-line}}]
                      (vec (cons head (cons ctx tail))))
        :else x))
    ast))

(defn- paragraph->text 
  "Transforms a PARAGRAPH node into a TEXT node."
  [[type ctx s]]
  (assert (= type :PARAGRAPH))
  (let [s' (subs s 3 (- (count s) 3))]  ; remove """ from both ends of s.
    (if-some [[_ space node-id text] (re-matches node-id-regex s')]
      [:TEXT (assoc ctx :node-id node-id) (str space (str/triml text))]
      [:TEXT ctx s'])))

(defn- sentence->text
  "Transforms a SENTENCE node into a TEXT node."
  [[type ctx s]]
  (assert (= type :SENTENCE))
  (if-some [[_ _ node-id text] (re-matches node-id-regex s)]
    [:TEXT (assoc ctx :node-id node-id) (str/trim text)]
    [:TEXT ctx (str/trim s)]))

(defn- normalize-text-nodes
  "In the following, we no longer want to differentiate between PARAGRAPH and
  SENTENCE nodes." 
  [ast]
  (w/postwalk
    (fn [x] 
      (cond
        (= :PARAGRAPH (u/node-type x)) (paragraph->text x)
        (= :SENTENCE (u/node-type x)) (sentence->text x) 
        :else x))
    ast))

(defn parse-diagram [x]
  (let [text (slurp x)]
    (->> (parse text)
         (insta/add-line-and-column-info-to-metadata text)
         (add-node-contexts)
         (normalize-text-nodes)
         (preprocess-diagram (.getParentFile (io/file x))))))

;(parse-diagram (io/resource "include/top.nassi"))
;(parse (slurp "/home/jan/tmp/pp.nassi" ))
;(parse-diagram (io/resource "diff/change-throw-a.nassi"))

;(clojure.pprint/pp)

; -------------------------------------------------------------

;; - Warnen, wenn eine Exception nicht behandelt wird.
