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

(defn assoc-some
  "Like `clojure.core/assoc`, but only for non-nil values.
   
   ```
   (assoc-some {:a 1} :b nil :c false :d 3) => {:a 1, :c false, :d 3}
   ```"
  ([m k v] 
   (if (some? v) (assoc m k v) m))
  ([m k v & kvs]
   (let [ret (assoc-some m k v)]
     (if kvs
       (if (next kvs)
         (recur ret (first kvs) (second kvs) (nnext kvs))
         (throw (IllegalArgumentException.
                  "assoc-some expects even number of arguments after map/vector, found odd number")))
       ret))))

(defn node-type 
  "A node is a vector whose first element is a keyword which corresponds with
  some left hand side value from the grammar.

  Returns said keyword, when `x` is a node."
  [x] 
  (when (vector? x) 
    (first x)))

(defn node? 
  "Returns true, if `x` is a node."
  [x]
  (keyword? (node-type x)))

(defn substr
  "Like `clojure.core/subs`, but also works with negative indexes (-1 is the
   index of the last char, -2 is the last but one index...))"
  ([s start] (substr s start (count s)))
  ([s start end]
   (let [normalize-index (fn [i] 
                           (if (neg? i) 
                             (+ (count s) i) i))]
     (subs s (normalize-index start) (normalize-index end)))))

(def ^:private 
  current-uid (atom 0))

(defn reset-uid!
  "We need this FN for test-cases.
  Test-cases need to reset the UIDs, in order to be deterministic."
  []
  (reset! current-uid 0))

(defn next-uid! []
  (swap! current-uid inc))

(defn third 
  "Returns the third item in the collection. Same as `(first (nnext coll))`.

   ```
   (third [1 2 3 4 5]) => 3
   (third [1 2]) => nil 
   (third nil) => nil
   ```"
  [coll]
  (first (nnext coll)))
