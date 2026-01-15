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
  "Embeds DIAGRAM nodes like so (note that only the root context is retained):

  ```
  (embed-diagrams [:DIAGRAM ctx-1 1 2 3 [:DIAGRAM ctx-2 4 5] 
                    [6 7 [8 9 [:DIAGRAM ctx-3 10]]] 11 12])
  => [:DIAGRAM ctx-1 1 2 3 4 5 [6 7 [8 9 10]] 11 12]
  ```"
  [xs] 
  (reduce (fn [ret x] 
            (cond 
              (= :DIAGRAM (u/node-type x)) (let [[tag ctx & tail] x]
                                             (u/concatv ret tail))
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

(defn- metadata 
  "If `ast` contains a METADATA node, returns it's METAINFO key/value pairs as
  a map."
  [ast]
  (when-some [metadata (u/third ast)]
    (when (= :METADATA (u/node-type metadata))
      (let [[_ _ & metainfos] metadata]
        (into {} (for [[_ _ k v] metainfos]
                   [k v]))))))

(defn- move-metadata-to-diagram-ctx
  "Removes the METADATA node from the `ast` and puts the METAINFO key/value
  pairs in the context map of the DIAGRAM node."
  [ast]
  (if-some [m (metadata ast)]
    (w/postwalk
      (fn [x] 
        (if (= :DIAGRAM (u/node-type x)) 
          (let [[tag ctx _ & tail] x]
            (apply vector tag (assoc ctx :meta m) tail))
          x))
      ast)
    ast))

(defn parse-diagram [x]
  (let [text (slurp x)
        ast (parse text)]
    (when (instance? instaparse.gll.Failure ast)
      (throw (ex-info "Parse error" {:failure ast})))
    (->> ast
         (insta/add-line-and-column-info-to-metadata text)
         (add-node-contexts)
         (normalize-text-nodes)
         (move-metadata-to-diagram-ctx)
         (preprocess-diagram (.getParentFile (io/file x))))))

#_(let [[_ {:keys [meta]} :as ast] (parse-diagram (io/resource "test/throw3.nassi"))] 
    ast)
    ;(clojure.pprint/pprint ast))
