(ns nassi.steps
  (:require [clojure.string :as str]
            [clojure.walk :as w]))

(defn- mk-step [] (atom [0]))

(defn- step-to-str [step]
  (str/join "." step))

(defn- inc-step! [step] 
  (->> (swap! step 
         (fn [s] (conj (pop s) (inc (last s)))))
       (str/join ".")))

(defn- push-step! [step]
  (swap! step (fn [s] (conj s 0))))

(defn- pop-step! [step]
  (swap! step (fn [s] (pop s))))

(defn- node-type 
  "A node is a vector whose first element is a keyword from the grammar.
  Returns said keyword, when `x` is a node."
  [x] 
  (when (vector? x) 
    (first x)))

(defn add-steps 
  "Adds steps to each SENTENCE- resp. PARAGRAPH node."
  [ast]
  (let [stp (mk-step)]
    (w/postwalk
      (fn [x] 
        (cond
          (= :CASES x)                  (do (push-step! stp) x)
          (= :CASES (node-type x))      (do (pop-step! stp) x)
          (= :BLOCK x)                  (do (push-step! stp) x)
          (= :BLOCK (node-type x))      (do (pop-step! stp) x)
          (= :ELSE x)                   (do (inc-step! stp) x)
          (= :SENTENCE (node-type x))   (conj x (inc-step! stp))
          (= :PARAGRAPH (node-type x))  (conj x (inc-step! stp))
          :else x))
      ast)))

(def ^:private node-id-regex  
  "Matches a pound-symbol followed by a valid HTML id.

  Valid HTML id attribute values begin with a letter and must comprise only
  letters ( a - Z ), digits ( 0 - 9 ), hyphens ( - ), underscores ( _ ), and
  periods ( . )." 
  #"\s*(#[A-Za-z][\.A-Za-z0-9_-]+).*")

(defn- node-id 
  "A SENTENCE or PARAGRAPH can optionally be annotated with an id (s.
  `node-id-regex`). A node-id must be the first thing in a SENTENCE resp.
  PARAGRAPH.
  
  e.g.

  ```
  (node-id [:SENTENCE \"#Check-1 In this step we check that, ...\"])
  => \"#Check-1\" 

  (node-id [:SENTENCE \"We copy the file and move on.\"])
  => nil
  ```"
  [[type s]]
  (when-some [[_ id] (re-matches node-id-regex s)]
    id))

(defn steps 
  "Returns a tuple `[new-ast id->step]`.
  
  In the `new-ast` a step number is added to each SENTENCE- and each PARAGRAPH
  node.
  
  The map `id->step` shows the determined step number for every defined id."

  [ast]
  (let [stp (mk-step)
        id->step (atom {})
        process-text (fn [x]
                       (let [curr-step (inc-step! stp)]
                         (when-some [id (node-id x)]
                           (swap! id->step assoc id curr-step))
                         (conj x curr-step)))]
    [(w/postwalk
      (fn [x] 
        (cond
          (= :CASES x)                  (do (push-step! stp) x)
          (= :CASES (node-type x))      (do (pop-step! stp) x)
          (= :BLOCK x)                  (do (push-step! stp) x)
          (= :BLOCK (node-type x))      (do (pop-step! stp) x)
          (= :ELSE x)                   (do (inc-step! stp) x)
          (= :SENTENCE (node-type x))   (process-text x)
          (= :PARAGRAPH (node-type x))  (process-text x)
          :else x))
      ast)
     @id->step]))
