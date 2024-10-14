(ns nassi.steps
  (:require 
    [nassi.stepper :as stepper]
    [nassi.util :as u]
    [clojure.string :as str]
    [clojure.walk :as w]))


(def ^:private node-id-regex  
  "Matches double exclamation marks followed by a valid HTML id at the start of
  a string (ignoring leading whitespace in front of the id).

  Valid HTML id attribute values begin with a letter and must comprise only
  letters ( a - Z ), digits ( 0 - 9 ), hyphens ( - ), underscores ( _ ), and
  periods ( . ).
  
  Returns three groups, if this regex matches:
  
    - The leading whitespace before the id.
    - The id itself.
    - The text following the id.

  Ex.

  ```
  (re-matches node-id-regex \"\\t!!ID-1 Hello, World! \")
  => [\"\\t!!ID-1 Hello, World! \" \"\\t\" \"!!ID-1\" \" Hello, World! \"]
  ```" 
  #"(?s)(\s*)(!![A-Za-z][\.A-Za-z0-9_-]+)(.*)")

(defn- process-sentence-node
  "Transforms a SENTENCE node like so:

     `[:SENTENCE <optional-id-and-text>]` 
  => `[:SENTENCE <optional-id> <step> <text>]`"
  [[type s] step]
  (assert (= type :SENTENCE))
  (if-some [[_ space id text] (re-matches node-id-regex s)]
    [type id step (str/trim text)]
    [type nil step (str/trim s)]))

(defn- process-paragraph-node
  "Transforms a PARAGRAPH node like so:

     `[:PARAGRAPH <optional-id-and-text>]` 
  => `[:PARAGRAPH <optional-id> <step> <text>]`"
  [[type s] step]
  (assert (= type :PARAGRAPH))
  (let [s2 (subs s 3 (- (count s) 3))] ; remove """ from both ends
    (if-some [[_ space id text] (re-matches node-id-regex s2)]
      [type id step (str space (str/triml text))]
      [type nil step s2])))   

(defn- process-errorcoderef-node
  "Transforms a ERRORCODE node like so:

     `[:ERRORCODEREF [:ERRORCODE <error-code>]]` 
  => `[:ERRORCODEREF <error-code> <step>]`"
  [[type [_ error-code]] stepper] 
  (assert (= type :ERRORCODEREF))
  [type error-code (stepper/fetch-step! stepper error-code)])

(defn add-steps 
  "Adds steps to each SENTENCE- resp. PARAGRAPH node."
  [ast]
  (let [stp (stepper/create)]
    (w/postwalk
      (fn [x] 
        (cond
          (= :CASES x)                      (do (stepper/add-step! stp) x)
          (= :CASES (u/node-type x))        (do (stepper/remove-step! stp) x)
          (= :BLOCK x)                      (do (stepper/add-step! stp) x)
          (= :BLOCK (u/node-type x))        (do (stepper/remove-step! stp) x)
          (= :ELSE x)                       (do (stepper/inc-step! stp) x)
          (= :SENTENCE (u/node-type x))     (process-sentence-node x 
                                              (stepper/inc-step! stp))
          (= :PARAGRAPH (u/node-type x))    (process-paragraph-node x 
                                              (stepper/inc-step! stp))
          (= :THROW (u/node-type x))        (let [error-code (second (second x))]
                                              (stepper/pin-step! stp error-code)
                                              x)
          (= :ERRORCODEREF (u/node-type x)) (process-errorcoderef-node x stp) 
          (= :HANDLE (u/node-type x))       (do (stepper/pop-step! stp) x)
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
        (when (or (= :SENTENCE  (u/node-type x))   
                  (= :PARAGRAPH (u/node-type x)))  
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
