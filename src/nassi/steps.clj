(ns nassi.steps
  (:require 
    [nassi.stepper :as stepper]
    [nassi.opts :as opts]
    [nassi.util :as u]
    [clojure.string :as str]
    [clojure.walk :as w]))

(defn- add-step-to-text-node-ctx!
  "Inserts the `step` into the context map `ctx`.
  As a side-effect, the map within the atom `node-id->step` gets updated."
  [[tag {:keys [node-id] :as ctx} s :as node] step node-id->step]
  (assert (= tag :TEXT))

  ;; update `node-id->step` map
  (when node-id
    (when-some [other-step (get @node-id->step node-id)]
      (throw (ex-info (str "ID " node-id " is not unique!")
               {:this-node node  
                :this-step step
                :other-step other-step})))
    (swap! node-id->step assoc node-id step))
  
  [tag (u/assoc-some ctx 
         ; transform `node-id` into a valid HTML-ID by removing "!!" from start
         :id (when node-id (subs node-id 2))
         :step step) s])

(defn- process-internal-links 
  "Internal links have the form `[!!id-of-something](#id-of-something)`. 
  This FN replaces the link-text of each internal link with its corresponding
  step number."
  [[tag ctx s] node-id->link-name]
  [tag ctx (reduce (fn [ret [node-id step]]
                     (str/replace ret (str "[" node-id "]") (str "[" step "]"))) 
             s node-id->link-name)])

(defn- process-errorcoderef-node!
  "Transforms a ERRORCODE node like so:

  `[:ERRORCODEREF {...} [:ERRORCODE _ <error-code>]]` 
  => `[:ERRORCODEREF {:step step, ...} <error-code>]`
  
  Also, this updates the atom `errorcode->id`."
  [[tag ctx [_ _ error-code]] stepper errorcode->id] 
  (assert (= tag :ERRORCODEREF))
  (let [id (str "nassi_errorcoderef_" (subs error-code 1))]
    (swap! errorcode->id assoc error-code id)
    [tag 
     (assoc ctx
       :id id
       :step (stepper/fetch-step! stepper error-code)) 
     error-code]))

(defn- add-link-target-id 
  "Adds the HTML identifier of the corresponding HANDLE to the `ctx` of this
  THROW node."
  [[tag ctx errorcode-node text-node] errorcode->id]
  (assert (= tag :THROW))
  (let [[_ _ errorcode] errorcode-node
        link-target-id (str "#" (get errorcode->id errorcode))]
    [tag (assoc ctx :link-target-id link-target-id) errorcode-node text-node]))

(defn add-steps 
  "Adds steps to each SENTENCE- resp. PARAGRAPH node."
  [ast]
  (let [{:keys [preamble epilogue]} opts/*gen-options*
        stp (stepper/create)
        node-id->step (atom {})
        ;; We need this to create hyperlinks from THROW to HANDLE sections.
        errorcode->id (atom {})]
    (->> ast
         (w/postwalk
           (fn [x] 
             (cond
               (= :PREAMBLE x)                   (do (stepper/disable! stp preamble) x)
               (= :PREAMBLE (u/node-type x))     (do (stepper/enable! stp) x)
               (= :EPILOGUE x)                   (do (stepper/disable! stp epilogue) x)
               (= :EPILOGUE (u/node-type x))     (do (stepper/enable! stp) x)
               (= :CASES x)                      (do (stepper/add-step! stp) x)
               (= :CASES (u/node-type x))        (do (stepper/remove-step! stp) x)
               (= :BLOCK x)                      (do (stepper/add-step! stp) x)
               (= :BLOCK (u/node-type x))        (do (stepper/remove-step! stp) x)
               (= :ELSE x)                       (do (stepper/inc-step! stp) x)
               (= :TEXT (u/node-type x))         (add-step-to-text-node-ctx! x 
                                                   (stepper/inc-step! stp)
                                                   node-id->step)
               (= :THROW (u/node-type x))        (let [[_ _ [_ _ error-code]] x]
                                                   (stepper/pin-step! stp error-code)
                                                   x)
               (= :ERRORCODEREF (u/node-type x)) (process-errorcoderef-node! x stp errorcode->id) 
               (= :HANDLE (u/node-type x))       (do (stepper/pop-step! stp) x)
               :else x)))
         (w/postwalk
           (fn [x] 
             (cond
               (= :TEXT (u/node-type x))         (process-internal-links x @node-id->step)
               (= :THROW (u/node-type x))        (add-link-target-id x @errorcode->id)               
               :else x))))))

