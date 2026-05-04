(ns nassi.stepper
  "This NS contains the mechanics to label the steps in a diagram."
  (:require 
    [clojure.string :as str]))

(defn create [] 
  (atom 
    {:stack [[0]]
     :pins {}}))

(defn- step-to-str [s]
  (str/join "." s))

(defn- swap-current-step! [x f]
  (swap! x 
    #(update % :stack
       (fn [stack]
         (conj (pop stack)
           (f (peek stack)))))))

(defn- current-step-to-string [x]
  (let [{:keys [disabled-const]} @x]
    (or disabled-const
      (step-to-str (peek (:stack @x))))))

(defn inc-step! 
  "Increment current step and return a string representation."
  [x]
  (swap-current-step! x 
    (fn [step] (conj (pop step) (inc (last step)))))
  (current-step-to-string x))
 
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
  "Save current step for later use via `fetch-step!` - but only, if there isn't
  already a pinned step under the same `label`."
  [x label]
  (swap! x 
    (fn [{:keys [stack pins] :as ret}]
      (if (contains? pins label)
        ret
        (update ret :pins #(assoc % label (peek stack)))))))

(defn fetch-step! 
  "Make the step, that was saved under `label` the current step and return a
  string representation.

  The previous step can be restored via `pop-step!`."
  [x label]
  (swap! x 
    (fn [{:keys [pins] :as ret}]
      (update ret :stack #(conj % (get pins label)))))
  (current-step-to-string x))

(defn pop-step! 
  "Discard the current step."
  [x]
  (swap! x 
    (fn [ret]
      (update ret :stack #(pop %)))))

(defn get-step 
  "Get a string representation of the step, that was saved under `label`.
  This FN has no side-effect."
  [x label]
  (step-to-str (get-in @x [:pins label])))

(defn contains-step? 
  "Returns true, if some step was pinned under the `label`."
  [x label]
  (boolean (get-in @x [:pins label])))

(defn disable! 
  "Disable stepper `x` temporarily, so that mutating FNs do not have any effect
  and return `const-str` as the string representation of the current step."
  [x const-str]
  (swap! x 
    (fn [ret]
      (-> ret
          (assoc  :disabled-const const-str)
          (update :stack #(conj % [Integer/MIN_VALUE]))))) ;; push dummy-step
  (current-step-to-string x))

(defn enable!
  "Enable stepper `x`."
  [x]
  (swap! x 
    (fn [ret]
      (dissoc ret :disabled-const)))
  (pop-step! x)) ;; pop dummy-step
