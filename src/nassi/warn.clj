(ns nassi.warn
  (:require 
    [clojure.set :as set]
    [clojure.walk :as w]
    [nassi.util :as u]))

(defn- linenumber 
  "Extracs the linenumber from a node context map."
  [{:keys [range]}]
  (:start-line range))

(defn- thrown+handled 
  "Returns a pair of maps `[thrown handled]` containing the thrown error-codes
  and the handled error-codes. Each map is a mapping `error-code->linenumber`."
  [ast]
  (let [thrown (atom {})
        handled (atom {})]
    (w/postwalk
      (fn [x] 
        (cond
          (= :THROW (u/node-type x))   (let [[_ _ [_ ctx error-code]] x
                                             ln (linenumber ctx)]
                                         (swap! thrown assoc error-code ln))
          (= :HANDLE (u/node-type x))  (let [[_ _ [_ _ [_ ctx error-code]]] x
                                             ln (linenumber ctx)]
                                         (swap! handled assoc error-code ln))
          :else x))
      ast)
    [@thrown @handled]))

(def ^:private template-warn-unhandled
  "Warning: No handler defined for error-code '%s' from line %d.")

(def ^:private template-warn-needless
  "Warning: error-code '%s' is handled in line %d, but there is no corresponding THROW statement with that particular error-code.")

(defn exception-warnings 
  "Returns a seq of warning messages about unhandled errors, as well as errors
  that are handled, but can never occur."
  [ast]
  (let [[thrown handled] (thrown+handled ast)
        unhandled (->> (set/difference (set (keys thrown)) (set (keys handled)))
                       (select-keys thrown)
                       (map #(apply format template-warn-unhandled %)))
        needless (->> (set/difference (set (keys handled)) (set (keys thrown)))
                      (select-keys handled)
                      (map #(apply format template-warn-needless %)))]
    (concat unhandled needless)))

#_(do 
    (require '[clojure.java.io :as io])
    (require '[nassi.parse :as p])
    
    (let [ast (p/parse-diagram (io/resource "test/throw2.nassi"))]
      (exception-warnings ast)))
