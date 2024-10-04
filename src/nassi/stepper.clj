(ns nassi.stepper
  "This NS contains the mechanics to label the steps in a diagram."
  (:require 
    [clojure.string :as str]))

(defn create [] 
  (atom 
    {:stack [[0]]
     :pins {}}))

(defn- swap-current-step! [x f]
  (swap! x 
    #(update % :stack
       (fn [stack]
         (conj (pop stack)
           (f (peek stack)))))))

(defn- step-to-string [x]
  (str/join "." (peek (:stack @x))))

(defn inc-step! 
  "Increment current step and return a string representation."
  [x]
  (swap-current-step! x 
    (fn [step] (conj (pop step) (inc (last step)))))
  (step-to-string x))
 
(defn add-step! 
  "Add a \"sub-step\" to the current step (e.g. 1.2 => 1.2.0)."
  [x]
  (swap-current-step! x 
    (fn [step] (conj step 0))))

(defn remove-step! 
  "Remove a \"sub-step\" from the current step (e.g. 1.2.1 => 1.2)."
  [x]
  (swap-current-step! x 
    (fn [step] (pop step))))

(defn pin-step! 
  "Save current step for later use via `fetch-step!`."
  [x label]
  (swap! x 
    (fn [{:keys [stack] :as ret}]
      (update ret :pins #(assoc % label (peek stack))))))

(defn fetch-step! 
  "Make the step, that was saved under `label` the current step and return a
  string representation."
  [x label]
  (swap! x 
    (fn [{:keys [stack pins] :as ret}]
      (update ret :stack #(conj % (get pins label)))))
  (step-to-string x))

(defn pop-step! 
  "Discard the current step."
  [x]
  (swap! x 
    (fn [{:keys [stack] :as ret}]
      (update ret :stack #(pop %)))))

(defn contains-step? 
  "Returns true, if some step was pinned under the `label`."
  [x label]
  (boolean (get-in @x [:pins label])))
