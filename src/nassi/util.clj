(ns nassi.util
  (:require [clojure.string :as str]))

(def concatv 
  "Like `clojure.core/concat`, but returns a vector."
  (comp (partial into []) concat))

(defn find-first 
  "Returns the first item from `coll`, which satisfies the predicate `pred`.

  ```
  (find-first odd? (range)) => 1

  (find-first even? [1 1 2 3 5]) => 2
  ```"
  [pred coll]
  (some #(when (pred %) %) coll))

(defn conj-some
  "Like `clojure.core/conj`, but only for non-nil values.
   
   ```
   (conj-some [] 1 nil 3 nil 5) => [1 3 5]
   ```"
  ([] [])
  ([coll] coll)
  ([coll x] (if x (conj coll x) coll))
  ([coll x & xs]
   (if xs
     (recur (conj-some coll x) (first xs) (next xs))
     (conj-some coll x))))

(defn assoc-some
  "Like `clojure.core/assoc`, but only for non-nil values.
   
   ```
   (assoc-some {:a 1} :b nil :c false :d 3) => {:a 1, :c false, :d 3}
   ```"
  ([m k v] 
   (if (some? v) (assoc m k v) m))
  ([m k v & kvs]
   (let [ret (assoc-some m k v)]
     (if kvs
       (if (next kvs)
         (recur ret (first kvs) (second kvs) (nnext kvs))
         (throw (IllegalArgumentException.
                  "assoc-some expects even number of arguments after map/vector, found odd number")))
       ret))))

(defn node-type 
  "A node is a vector whose first element is a keyword which corresponds with
  some left hand side value from the grammar.

  Returns said keyword, when `x` is a node."
  [x] 
  (when (vector? x) 
    (first x)))

(defn node? 
  "Returns true, if `x` is a node."
  [x]
  (keyword? (node-type x)))

(defn substr
  "Like `clojure.core/subs`, but also works with negative indexes (-1 is the
   index of the last char, -2 is the last but one index...))"
  ([s start] (substr s start (count s)))
  ([s start end]
   (let [normalize-index (fn [i] 
                           (if (neg? i) 
                             (+ (count s) i) i))]
     (subs s (normalize-index start) (normalize-index end)))))

(def ^:private 
  current-uid (atom 0))

(defn reset-uid!
  "We need this FN for test-cases.
  Test-cases need to reset the UIDs, in order to be deterministic."
  []
  (reset! current-uid 0))

(defn next-uid! []
  (swap! current-uid inc))

(defn third 
  "Returns the third item in the collection. Same as `(first (nnext coll))`.

   ```
   (third [1 2 3 4 5]) => 3
   (third [1 2]) => nil 
   (third nil) => nil
   ```"
  [coll]
  (first (nnext coll)))

(defn repeat-str 
  "Like `clojure.core/repeat`, but for strings.
   
   ```
   (repeat-str 2 \"ab\") => \"abab\"
   ```
   "
  [n s]
  (apply str (repeat n s)))

(defn split-words
  "Splittet die Seq `words` in ein Tuppel auf, so dass der erste Teil einen
  String mit den Woertern enthaelt, deren Gesamtlaenge (zzgl. einem Space
  zwischen jedem Wort) nicht das `limit` ueberschreiten. Der zweite Teil
  enthaelt die uebrig gebliebenen Worte."
  [words limit & {:keys [trim?] 
                  :or {trim? true}}]
  (loop [len 0, front [], back words]
    (cond
      (= len (inc limit)) [(cond-> (str/join " " front) trim? str/trim) back]
      (> len (inc limit)) [(cond-> (str/join " " (butlast front)) trim? str/trim) 
                           (cons (last front) back)]
      :else (if-some [w (first back)]
              (recur (inc (+ len (.length w))) (conj front w) (next back))
              [(str/join " " front) back]))))

(defn word-wrap 
  "Liefert einen String zurueck, dessen Zeilen maximal `limit` Zeichen lang
  sind. Das gilt allerdings nur, sofern der uebergebene String `s` keine
  Woerter enthaelt die laenger als das `limit` sind. Wenn ein `margin`
  angegeben wurde, werden am linken Rand entsprechend viele Leerzeichen
  eingefuegt."

  ([s limit] (word-wrap s limit 0))
  ([s limit margin]
   (assert (> limit margin))
   (let [padding (repeat-str margin " ")
         line (str/replace s #"\n[\s]*" " ")]
     (loop [buf [] 
            words (enumeration-seq (java.util.StringTokenizer. line))]
       (let [[s rest :as normal] (split-words words (- limit margin))
             ;; Hier behandeln wir den pathologischen Fall, wenn ein einzelnes
             ;; Wort laenger als das `limit` ist.
             [s' rest'] (if (and (str/blank? s) (first rest))
                          [(first rest) (next rest)]
                          normal)
             ret (conj buf (str padding s'))]
         (if-some [zs (seq rest')]
           (recur ret rest')
           (str/join "\n" ret)))))))
