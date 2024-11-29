(ns nassi.main
  (:require 
    [nassi.diff :as diff]
    [instaparse.failure :as failure]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.cli :as cli]
    [nassi.parse :as p]
    [nassi.html :as html])
  (:gen-class))

(def ^:private spaces "                                                    ")

(def ^:private cli-options
  [["-o" "--output <file>" "Set output file name."]
   ["-d" "--diff <file>" "File name of original file."]
   [nil "--[no-]show-metadata" "Render metadata?" :default true]
   [nil "--metadata-pos POS" (str "Show the metadata table\n"
                               spaces "above (top) or below (bottom)\n" spaces
                               "the diagram?")
    :default "top"
    :validate [#{"top" "bottom"} "Must be either 'top' or 'bottom'."]]
   ["-k" "--metadata-key KEY" (str "Metadata keys of interest.\n" spaces 
                                "When not specified, all\n" spaces 
                                "key/value pairs are rendered.")
    :multi true
    :default []
    :update-fn conj]
   [nil "--opt-bgcol-change COLOR" (str "HTML background-color(-code)\n" spaces 
                                     "for diff changes.")
    :default "yellow"] 
   [nil "--opt-bgcol-insert COLOR" (str "HTML background-color(-code)\n" spaces 
                                     "for diff inserts")
    :default "lightgreen"] 
   [nil "--opt-bgcol-delete COLOR" (str "HTML background-color(-code)\n" spaces 
                                     "for diff deletes")
    :default "lightpink"] 
   [nil "--opt-true LABEL" (str "Set label for true-branches\n" spaces "in if-statements.")
    :default "Yes"] 
   [nil "--opt-false LABEL"(str "Set label for false-branches\n" spaces "in if-statements.")
    :default "No"] 
   [nil "--opt-catch LABEL" "Set label for exception handling."
    :default "Exception-Handling"] 
   ["-h" "--help" "Show this help and exit."]])

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
    (str/join \newline errors)))

(defn- usage [options-summary]
  (->> 
    [(str "NASSI " (str/trim (slurp (io/resource "VERSION")))
       " - Generate beautiful nassi-shneiderman diagrams.")
     "Copyright (c) 2024 Phoenix Reisen GmbH." 
     "License: BSD 3-Clause (see file LICENSE)."
     "https://github.com/phoenixreisen/nassi"
     ""
     "Usage: nassi.jar [options] input-file"
     "       nassi.jar [options] --diff original-file revised-file"
     "Options:" options-summary]
    (str/join \newline)))

(defn- validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map with
  the input-file and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} 
        (cli/parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}

      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

      ;; custom validation on arguments
      (= 1 (count arguments))
      {:input-file (first arguments) :options options}

      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn- generate-html-file [input-file {:keys [diff opt-bgcol-change
                                              opt-bgcol-delete opt-bgcol-insert
                                              opt-catch opt-false opt-true
                                              output show-metadata metadata-pos
                                              metadata-key]}]
  (with-bindings {#'nassi.html/*gen-options* 
                  {:true            opt-true
                   :false           opt-false
                   :catch           opt-catch
                   :diff-change-bg  opt-bgcol-change
                   :diff-insert-bg  opt-bgcol-insert
                   :diff-delete-bg  opt-bgcol-delete}}
    (let [opts {:show-metadata    show-metadata
                :metadata-at-top  (= metadata-pos "top")
                :metadata-keyseq  metadata-key}
          html (if diff
                 (diff/to-html diff input-file)
                 (html/to-html (p/parse-diagram input-file) opts))]
      (if output
        (spit output html)
        (println html)))))

(defn -main [& args]
  (let [{:keys [input-file options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (try 
        (generate-html-file input-file options)
        (catch Exception e 
          (if-some [{:keys [failure]} (ex-data e)]
            (failure/pprint-failure failure)
            ; This should never happen
            ; TODO We should ask the user to send us a bug report!
            (.printStackTrace e)))))))

#_(try 
    (generate-html-file "resources/test/include1.uc"      
      {:output "/home/jan/repos/phoenixreisen/nassi/t.html"
       :inline-css true
       :opt-true "Ja"
       :opt-false "Nein"
       :opt-catch "Behandlung der Ausnahmen"
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

;;TODO define CLI option for metadata
;;TODO describe metadata feature in README
