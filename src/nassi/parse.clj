(ns nassi.parse
  (:require 
    [clojure.java.io :as io]
    [clojure.walk :as w]
    [instaparse.core :as insta]
    [nassi.util :as u]))

(def ^:private parse 
  "This FN accepts a textual representation of a nassi-shneiderman diagram and
  returns either an abstract syntax tree, or a failure."
  (insta/parser (io/resource "grammar.bnf") :string-ci true))

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
          (let [[_ [_ fname]] x
                f (io/file working-dir (u/substr fname 1 -1))]
            (parse-diagram f))
          x)) 
      ast)))
  
(defn parse-diagram [x]
  (->> (parse (slurp x))
       (preprocess-diagram (.getParentFile (io/file x)))))

; -------------------------------------------------------------

;; - Warnen, wenn eine Exception nicht behandelt wird.

(comment
  (require '[editscript.core :as e])

  (def a (parse (slurp (io/resource "ex0.uc"))))
  (def b (parse (slurp (io/resource "ex1.uc"))))
  (e/diff a b)

  (parse-diagram  "resources/test/include.uc")
  (parse-diagram (io/resource "test/throw1.uc"))

)

