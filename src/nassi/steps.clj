(ns nassi.steps
  (:require 
    [clojure.string :as str]
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
  "A node is a vector whose first element is a keyword which corresponds with
  some left hand side value from the grammar.

  Returns said keyword, when `x` is a node."
  [x] 
  (when (vector? x) 
    (first x)))

(def ^:private node-id-regex  
  "Matches a pound-symbol followed by a valid HTML id.

  Valid HTML id attribute values begin with a letter and must comprise only
  letters ( a - Z ), digits ( 0 - 9 ), hyphens ( - ), underscores ( _ ), and
  periods ( . )." 
  #"(?s)\s*(#[A-Za-z][\.A-Za-z0-9_-]+).*")

(defn- process-sentence-node
  "Transforms a SENTENCE node like so:

     `[:SENTENCE <optional-id-and-text>]` 
  => `[:SENTENCE <optional-id> <step> <text>]` 

  e.g.

  ```
  (process-sentence-node [:SENTENCE \" #x1-2 Bla, bla, bla\"] \"1.2\")
  => [:SENTENCE \"#x1-2\" \"1.2\" \"Bla, bla, bla\"]

  (process-sentence-node [:SENTENCE \"Bla, bla, bla\"] \"1.2\")
  => [:SENTENCE nil \"1.2\" \"Bla, bla, bla\"]
  ```"
  [[type s] step]
  (if-some [[_ id] (re-matches node-id-regex s)]
    [:SENTENCE id step (-> (str/replace-first s id "")
                           (str/trim))]
    [:SENTENCE nil step (str/trim s)]))

(defn- process-paragraph-node
  "Transforms a PARAGRAPH node like so:

     `[:PARAGRAPH <optional-id-and-text>]` 
  => `[:PARAGRAPH <optional-id> <step> <text>]`"
  [[type s] step]
  (let [s2 (subs s 3 (- (count s) 3))] ; remove """ from both ends
    (if-some [[_ id] (re-matches node-id-regex s2)]
      [:PARAGRAPH id step (str/replace-first s2 id "")]
      [:PARAGRAPH nil step s2])))   

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
          (= :SENTENCE (node-type x))   (process-sentence-node x (inc-step! stp))
          (= :PARAGRAPH (node-type x))  (process-paragraph-node x (inc-step! stp))
          :else x))
      ast)))

(defn id->step
  "Maps each id to it's corresponding step. Therefore this FN should only be
  called, if `ast` was delivered via `add-steps`.

  Throws an Exception, if the same id is found in more than one node."
  [ast]
  (let [res (atom {})]
    (w/postwalk
      (fn [x] 
        (when (or (= :SENTENCE  (node-type x))   
                  (= :PARAGRAPH (node-type x)))  
          (let [[_ id step _] x]
            (when id
              (when-some [node (get @res id)]
                (throw (ex-info (str "ID " id " is not unique!")
                         {:a node
                          :b x})))
              (swap! res assoc id step))))
        x)
      ast)
    @res))

(comment TEST-CODE
(let [s1  "   #Check-1\n In this step we check that, ..."
      s2  "\"\"\" #Zaehne\nZahnpflege:\n- Zahnseide verwenden\n- Zaehne gruendlich putzen\n- Zahnzwischenraumbuerstchen benutzen\n- Mundspuelung\"\"\""
     s "hello";s1; (subs s2 3) 
     [_ id] 
(re-matches node-id-regex s)
      ]
  id)

(process-sentence-node [:SENTENCE " #x1-2 Bla, bla, bla"] "1.2")

  (= (process-sentence-node [:SENTENCE "Bla, bla, bla"] "1.2")
   [:SENTENCE nil "1.2" "Bla, bla, bla"])


  (process-sentence-node [:SENTENCE "   #Check-1 In this step we check that, ..."])
  (process-sentence-node [:SENTENCE " In this step we check that, ..."])
  (process-paragraph-node
[:PARAGRAPH "\"\"\" #Zaehne\nZahnpflege:\n- Zahnseide verwenden\n- Zaehne gruendlich putzen\n- Zahnzwischenraumbuerstchen benutzen\n- Mundspuelung\"\"\""] "1.3")

  (process-paragraph-node
    [:PARAGRAPH "\"\"\"\nZahnpflege:\n- Zahnseide verwenden\n- Zaehne gruendlich putzen\n- Zahnzwischenraumbuerstchen benutzen\n- Mundspuelung\"\"\""] "1.2")
  (node-id
    [:PARAGRAPH "\"\"\" #Zaehne\nZahnpflege:\n- Zahnseide verwenden\n- Zaehne gruendlich putzen\n- Zahnzwischenraumbuerstchen benutzen\n- Mundspuelung\"\"\""])
)
