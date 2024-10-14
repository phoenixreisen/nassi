(ns nassi.util
  (:require [clojure.string :as str]))

(def concatv 
  "Like `clojure.core/concat`, but returns a vector."
  (comp (partial into []) concat))

(defn find-first 
  "Returns the first item from `coll`, which satisfies the predicate `pred`.

  ```
  (find-first odd? (range)) => 1

  (find-first even? [1 1 2 3 5]) => 2
  ```"
  [pred coll]
  (some #(when (pred %) %) coll))

(defn node-type 
  "A node is a vector whose first element is a keyword which corresponds with
  some left hand side value from the grammar.

  Returns said keyword, when `x` is a node."
  [x] 
  (when (vector? x) 
    (first x)))

(defn substr
  "Like `clojure.core/subs`, but also works with negative indexes (-1 is the
   index of the last char, -2 is the last but one index...))"
  ([s start] (substr s start (count s)))
  ([s start end]
   (let [normalize-index (fn [i] 
                           (if (neg? i) 
                             (+ (count s) i) i))]
     (subs s (normalize-index start) (normalize-index end)))))
