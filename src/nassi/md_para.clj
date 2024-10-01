(ns nassi.md-para
  "This NS contains FNs to prepare PARAGRAPH:s for markdown transformation."
  (:require 
    [clojure.string :as str]
    [nassi.util :as u]))

(defn- first-non-empty-line-after-start 
  "Returns the first non-empty line after the starting line (the one beginning
  with \"\"\") of PARAGRAPH s."
  [s]
  (->> (str/split-lines s)
       (drop 1)
       (u/find-first (complement str/blank?))))

(defn- leading-spaces+rest 
  "Returns the leading spaces of `s` and the rest of the string as a pair
  `[leading-spaces rest]` (tabs are converted to four spaces).
  
  Example:
  ```
  (leading-spaces+rest \"\\t Hello, world!\")
  => [\"     \" \"Hello, world!\"] 
  ```"
  [s]
  (let [[_ leading-whitespace rest] (re-matches #"(\s*)(.*)" s)
        leading-spaces (str/replace leading-whitespace "\t" "    ")]
    [leading-spaces rest]))

(defn- drop-leading-spaces 
  "Drops the first `n` leading spaces from string `s`."
  [s n]
  (let [[leading-spaces rest] (leading-spaces+rest s)]
    (str
      (subs leading-spaces (min n (count leading-spaces)))
      rest)))

(defn trim-paragraph [s]
  (if-some [line (first-non-empty-line-after-start s)]
    (let [n (-> (leading-spaces+rest line) (first) (count))
          xs (str/split-lines s)]
      (reduce (fn [ret x] (str ret \newline (drop-leading-spaces x n)))
        (first xs) (rest xs)))
    s))
