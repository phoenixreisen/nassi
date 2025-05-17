(ns nassi.pp
  (:require 
    [clojure.string :as str]
    [instaparse.core :as insta]))

(defn- xf-diagram [ctx & xs] xs)

(defn- xf-block [ctx & xs] 
  (str "{" \newline (str/join xs) "}" \newline))

(defn- xf-sub [ctx text block] 
  (str "SUB" \newline text block))

(defn- xf-for [ctx text block] 
  (str "FOR" \newline text block))

(defn- xf-while [ctx text block] 
  (str "WHILE" \newline text block))

(defn- xf-until [ctx text block] 
  (str "UNTIL" \newline text block))

(defn- xf-switch [ctx text cases] 
  (str "SWITCH" \newline text cases))

(defn- xf-cases [ctx & xs] 
  (str "{" \newline (str/join xs) "}" \newline))

(defn- xf-case [ctx text block] 
  (str "CASE" \newline text block))

(defn- xf-default [ctx text block] 
  (str "DEFAULT" \newline  text block))

(defn- xf-textstmt [ctx text] text)

(defn- xf-text [{:keys [node-id]} s] 
  (str "\"\"\"" (when node-id (str node-id " ")) s "\"\"\"" \newline))

(defn- xf-else [ctx block] 
  (str "ELSE" \newline block))

(defn- xf-preamble [ctx text]
  (str "PREAMBLE" \newline text))

(defn- xf-epilogue [ctx text]
  (str "EPILOGUE" \newline text))

(defn- xf-if 
  ([ctx text block] 
   (str "IF" \newline text block))
  ([ctx text block else]
   (str "IF" \newline text block else)))

(defn- xf-error-code [ctx s] 
  (str s \newline))

(defn- xf-throw [ctx error-code text] 
  (str "THROW" \newline error-code text))

(defn- xf-catch [ctx handlers] 
  (str "CATCH" \newline "{" \newline handlers "}" \newline))

(defn- xf-handlers [ctx & xs] 
  (str/join xs))

(defn- xf-errorcoderef [ctx error-code] error-code)

(defn- xf-handle [ctx error-code block] 
  (str "HANDLE" \newline error-code block))

(defn to-flat-str [ast]
  (apply str
    (insta/transform 
      {:DIAGRAM     xf-diagram
       :PREAMBLE    xf-preamble
       :EPILOGUE    xf-epilogue
       :TEXTSTMT    xf-textstmt
       :TEXT        xf-text
       :BLOCK       xf-block
       :SUB         xf-sub
       :FOR         xf-for
       :WHILE       xf-while
       :UNTIL       xf-until
       :SWITCH      xf-switch
       :CASES       xf-cases
       :CASE        xf-case
       :DEFAULT     xf-default
       :ELSE        xf-else
       :IF          xf-if
       :THROW       xf-throw
       :CATCH       xf-catch
       :HANDLERS    xf-handlers
       :HANDLE      xf-handle
       :ERRORCODEREF xf-errorcoderef
       :ERRORCODE   xf-error-code
       } 
      ast)))
