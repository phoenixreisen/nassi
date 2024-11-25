(ns nassi.diff
  (:import 
    [java.io File]
    [com.github.difflib DiffUtils]
    [com.github.difflib.patch AbstractDelta Chunk DeltaType Patch])
  (:require 
    [nassi.pp :as pp]
    [clojure.string :as str]
    [markdown.core :as md]
    [nassi.md-para :as md-para]
    [instaparse.core :as insta]
    [hiccup.core :as h]
    [clojure.java.io :as io]
    [clojure.walk :as w]
    [nassi.parse :as p]
    [nassi.steps :as steps]
    [nassi.transform :as xf]
    [nassi.util :as u]))

(defn- create-temp-file []
  (doto (File/createTempFile "nassi" nil)
    (.deleteOnExit)))

(defn- chunk->range [^Chunk chunk]
  (let [pos (.getPosition chunk)
        size (.size chunk)]
    {:start-line (inc pos)
     :end-line (+ pos size)}))

(defn- covers? 
  "Returns true, if `range1` covers `range2`."
  [range1 range2]
  (and
    (<= (:start-line range1) (:start-line range2))
    (>= (:end-line range1) (:end-line range2))))

(defn- fits? 
  "Returns true, if the `chunk` fits in the `range`."
  [^Chunk chunk range]
  (covers? range (chunk->range chunk)))

(defn- range-size [{:keys [start-line end-line] :as range}]
  (if (some? range)
    (inc (- end-line start-line))
    Integer/MAX_VALUE))

(defn- best-fit 
  "Returns the uid of the best fitting `[range uid]` pair for the `chunk`."
  [range->uid chunk]
  (second 
    (reduce 
      (fn [[ret-range ret-uid :as a] [range uid :as b]]
        (if (and (fits? chunk range)
              (< (range-size range) (range-size ret-range)))
          b a))
      nil range->uid)))

(defn- uid [node]
  (when (u/node? node)
    (let [[_ {:keys [uid]}] node]
      uid)))

(defn- assoc-ctx 
  "assoc[iates] `key` and `val` to the context map of each node, whose uid is
  in the set `uids`."
  [ast uids key val]
  (w/postwalk
    (fn [n] 
      (if (contains? uids (uid n))
        (update n 1 assoc key val)
        n))
    ast))

(defn- range->uid [ast]
  (let [ret (atom {})]
    (w/postwalk
      (fn [n] 
        (when (contains? #{:CASE :CATCH :DEFAULT :ELSE :FOR :HANDLE :IF :SUB
                           :SWITCH :TEXT :THROW :UNTIL :WHILE}
                (u/node-type n))
          (let [[tag {:keys [range uid]} x & xs] n]
            (swap! ret assoc range uid)))
        n)
      ast)
    @ret))

(defn- split-chunk [^Chunk chunk]
  (let [pos (atom (dec (.getPosition chunk)))
        next-pos #(swap! pos inc)]
    (map #(new Chunk (int (next-pos)) (list %))
      (.getLines chunk))))

(defn- noise? 
  "Returns true, if `chunk` has only a single line containing \"{\" or \"}\"."
  [^Chunk chunk]
  (let [lines (.getLines chunk)]
    (and (= 1 (count lines))
      (contains? #{"{" "}"} (first lines)))))

(defn- source-chunks [deltas delta-type]
  (->> (filter #(= (.getType %) delta-type) deltas)
       (map #(.getSource %))
       (mapcat split-chunk)
       (remove noise?)))

(defn- target-chunks [deltas delta-type]
  (->> (filter #(= (.getType %) delta-type) deltas)
       (map #(.getTarget %))
       (mapcat split-chunk)
       (remove noise?)))

(defn- normalize-line 
  "Wir wollen weder Whitespace noch Node-IDs in den Diff einbeziehen."
  [s]
  (let [s1 (cond-> s (str/starts-with? s "\"\"\"") (subs 3))
        s2 (if-some [[_ _ _ text] (re-matches p/node-id-regex s1)]
             text s1)]
    (str/trim s2)))

(defn- asts+deltas [a b]
  (let [flat-a (pp/to-flat-str (p/parse-diagram a))
        flat-b (pp/to-flat-str (p/parse-diagram b))
        
        outfile-a (create-temp-file)
        outfile-b (create-temp-file)

        _ (spit outfile-a flat-a)
        _ (spit outfile-b flat-b)

        ast-a (p/parse-diagram outfile-a)
        ast-b (p/parse-diagram outfile-b)

        deltas (-> (DiffUtils/diff 
                     (map normalize-line (str/split-lines flat-a))
                     (map normalize-line (str/split-lines flat-b))) 
                   (.getDeltas))]
;    (println outfile-a)
;    (println outfile-b)
;    (doseq [d deltas] (println d))
    [ast-a ast-b deltas]))

(defn- left-iframe [ast]
  (str
    "<iframe frameborder=\"0\" scrolling=\"yes\" height=\"100%\" width=\"49%\" 
      align=\"left\" style=\"height: 100%; width: 49%; float: left;\" srcdoc=\"" 
    (str/replace
      (str/replace (xf/to-html ast) 
        "\"" "&quot;")
      "\n" " ")
    "\"></iframe>"))

(defn- right-iframe [ast]
  (str
    "<iframe frameborder=\"0\" scrolling=\"yes\" height=\"100%\" width=\"49%\" 
      align=\"right\" style=\"overflow: hidden; height: 100%; width: 49%;\" 
      srcdoc=\"" 
    (str/replace
      (str/replace (xf/to-html ast) 
      "\"" "&quot;")
      "\n" " ")
    "\"></iframe>"))

(defn to-html 
  "Generates a HTML representation of the diff between `a` and `b`."
  [a b]
  (let [[ast-a ast-b deltas] (asts+deltas a b) 
        
        range->uid-a (range->uid ast-a)
        range->uid-b (range->uid ast-b)

        uids-a-delete (map (partial best-fit range->uid-a)
                        (source-chunks deltas DeltaType/DELETE))
        uids-a-change (map (partial best-fit range->uid-a)
                        (source-chunks deltas DeltaType/CHANGE))
        uids-b-change (map (partial best-fit range->uid-b)
                        (target-chunks deltas DeltaType/CHANGE))
        uids-b-insert (map (partial best-fit range->uid-b)
                        (target-chunks deltas DeltaType/INSERT)) 

        res-ast-a (-> ast-a 
                      (assoc-ctx (set uids-a-delete) :diff-style :style-diff-delete)
                      (assoc-ctx (set uids-a-change) :diff-style :style-diff-change-delete))

        res-ast-b (-> ast-b 
                      (assoc-ctx (set uids-b-insert) :diff-style :style-diff-insert)
                      (assoc-ctx (set uids-b-change) :diff-style :style-diff-change)) ]
    (str
      "<!DOCTYPE html><html><head>"
      "<style>html,body{ width: 100%; height: 100%;}</style></head><body>"
      (left-iframe res-ast-a)
      (right-iframe res-ast-b)
      "</body></html>")))
