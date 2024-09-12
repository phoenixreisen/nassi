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
