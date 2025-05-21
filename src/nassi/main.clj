(ns nassi.main
  (:require 
    [nassi.opts :as opts]
    [nassi.warn :as w]
    [nassi.util :as u]
    [nassi.diff :as diff]
    [instaparse.failure :as failure]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.cli :as cli]
    [nassi.parse :as p]
    [nassi.html :as html])
  (:gen-class))

(def ^:private cli-options
  [["-o" "--output FILE" "Write output to FILE."]
   ["-d" "--diff FILE" "Use FILE as original file."]
   [nil "--[no-]show-metadata" "Render metadata?" :default true]
   ["-k" "--metadata-key KEY" 
    "Metadata keys of interest (use one option per KEY). 
    When not specified, all key/value pairs are rendered."
    :multi true
    :default []
    :update-fn conj]
   [nil "--metadata-pos [top|bottom]" 
    "Display metadata above (top) or below (bottom) the diagram?"
    :default "top"
    :validate [#{"top" "bottom"} "Must be either 'top' or 'bottom'."]]
   [nil "--opt-bgcol-change COLOR" "Use COLOR as background-color for diff changes."
    :default "yellow"] 
   [nil "--opt-bgcol-insert COLOR" "Use COLOR as background-color for diff inserts."
    :default "lightgreen"] 
   [nil "--opt-bgcol-delete COLOR" "Use COLOR as background-color for diff deletes"
    :default "lightpink"] 
   [nil "--opt-true TEXT" "Use TEXT as label for true-branches in if-statements."
    :default "Yes"] 
   [nil "--opt-false TEXT" "Use TEXT as label for false-branches in if-statements."
    :default "No"] 
   [nil "--opt-catch TEXT" "Use TEXT as label for exception handling."
    :default "Exception-Handling"] 
   [nil "--opt-preamble TEXT" "Use TEXT as label for links to the preamble section."
    :default "Preamble"] 
   [nil "--opt-epilogue TEXT" "Use TEXT as label for links to the epilogue section."
    :default "Epilogue"] 
   [nil "--version" "Show version information."]
   ["-h" "--help" "Show this help."]])

(defn- cli-option->str
  [[short-param long-param descr] & {:keys [margin] 
                                     :or {margin 28}}]
  (let [params (str (if (str/blank? short-param) 
                      (u/repeat-str 6 " ")
                      (str "  " short-param ", ")) 
                 long-param)
        padding (when (< (count params) (dec margin))
                  (u/repeat-str (- (dec margin) (count params)) " "))
        words (cons (str params padding)
                (enumeration-seq (java.util.StringTokenizer. descr)))
        [s ws] (u/split-words words 80 :trim? false)
        ss (when (seq ws)
             (u/word-wrap (str/join " " ws) 80 margin))]
    (str/join \newline (u/conj-some [] s ss))))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
    (str/join \newline errors)))

(def ^:private version 
  (str/join \newline
    [(str "nassi " (str/trim (slurp (io/resource "VERSION"))))
     "Copyright (c) 2024 Phoenix Reisen GmbH." 
     "License: BSD 3-Clause (see file LICENSE on website)."
     "https://github.com/phoenixreisen/nassi"]))

(def ^:private usage 
  (->> 
    [version
     ""
     "Usage: nassi.jar [options] input-file"
     "       nassi.jar [options] --diff original-file revised-file"
     "Options:" 
     (str/join \newline
       (map cli-option->str cli-options))]
    (str/join \newline)))

(defn- validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map with
  the input-file and the options provided."
  [args]
  (let [{:keys [options arguments errors _summary]} 
        (cli/parse-opts args cli-options)]
    (cond
      (:help options)     {:exit-message usage :ok? true}
      (:version options)  {:exit-message version :ok? true}
      errors              {:exit-message (error-msg errors)}
      (= 1 (count arguments)) {:input-file (first arguments) :options options}
      :else               {:exit-message usage})))

(defn generate-html-file [input-file {:keys [diff 
                                              opt-bgcol-change
                                              opt-bgcol-delete 
                                              opt-bgcol-insert
                                              opt-catch 
                                              opt-false 
                                              opt-true
                                              opt-preamble
                                              opt-epilogue
                                              output 
                                              show-metadata 
                                              metadata-pos
                                              metadata-key]}]
  (with-bindings {#'nassi.opts/*gen-options* 
                  {:true            opt-true
                   :false           opt-false
                   :catch           opt-catch
                   :preamble        opt-preamble
                   :epilogue        opt-epilogue
                   :diff-change-bg  opt-bgcol-change
                   :diff-insert-bg  opt-bgcol-insert
                   :diff-delete-bg  opt-bgcol-delete}}
    (let [opts {:show-metadata    show-metadata
                :metadata-at-top  (= metadata-pos "top")
                :metadata-keyseq  metadata-key}
          warnings (atom nil)
          html (if diff
                 (diff/to-html diff input-file)
                 (let [ast (p/parse-diagram input-file)]
                   (reset! warnings (seq (w/exception-warnings ast)))
                   (html/to-html (p/parse-diagram input-file) opts)))]
      (if output
        (spit output html)
        (println html))
      @warnings)))

;; TODO 
;; - DEFAULT ohne Text scheint nicht zu funktionieren
;; - Java-Interface 
;; - Mehrere Specs auf einen Schlag generieren (s. Routenplanung)
(defn -main [& args]
  (let [{:keys [input-file options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (try 
        (when-some [warnings (generate-html-file input-file options)]
          (doseq [w warnings] (println w)))
        (generate-html-file input-file options)
        (catch clojure.lang.ExceptionInfo e
          (if-some [{:keys [failure]} (ex-data e)]
            (failure/pprint-failure failure)
            (.printStackTrace e)))
        (catch java.io.FileNotFoundException e 
          (println (.getMessage e)))))))

#_(try 
    (generate-html-file "resources/test/preamble.uc"      
      {:output "/home/jan/repos/phoenixreisen/nassi/t.html"
       :inline-css true
       :opt-true "Ja"
       :opt-false "Nein"
       :opt-catch "Behandlung der Ausnahmen"
       :opt-preamble "Einleitung"
       :opt-epilogue "Epilog"
       })
    (catch Exception x (.printStackTrace x)))

#_(try 
    (generate-html-file "examples/ex2b.nassi"
      {:diff "examples/ex1.nassi" ;; original
       :opt-bgcol-change "yellow"
       :opt-bgcol-insert "lightgreen"
       :opt-bgcol-delete "lightpink"
       :output "/home/jan/repos/phoenixreisen/nassi/t.html"
       :inline-css true
       :opt-true "Ja"
       :opt-false "Nein"
       :opt-catch "Behandlung der Ausnahmen"
       })
    (catch Exception x (.printStackTrace x)))
