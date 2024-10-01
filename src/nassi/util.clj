(ns nassi.util
  (:require [clojure.string :as str]))

(defn find-first 
  "Returns the first item from `coll`, which satisfies the predicate `pred`.

  ```
  (find-first odd? (range)) => 1

  (find-first even? [1 1 2 3 5]) => 2
  ```"
  [pred coll]
  (some #(when (pred %) %) coll))
