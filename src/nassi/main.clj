(ns nassi.main
  (:require 
    [nassi.diff :as diff]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.cli :as cli]
    [nassi.parse :as p]
    [nassi.transform :as xf])
  (:gen-class))

(def ^:private cli-options
  [["-o" "--output <file>" "Set output file name."]
   ["-d" "--diff <file>" "File name of original file."]
   [nil "--opt-bgcol-change COLOR" "HTML background-color(-code) for diff changes."
    :default "yellow"] 
   [nil "--opt-bgcol-insert COLOR" "HTML background-color(-code) for diff inserts."
    :default "lightgreen"] 
   [nil "--opt-bgcol-delete COLOR" "HTML background-color(-code) for diff deletes."
    :default "lightpink"] 
   [nil "--opt-true LABEL" "Set label for true-branches in if-statements."
    :default "Yes"] 
   [nil "--opt-false LABEL" "Set label for false-branches in if-statements."
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

(defn- generate-html-file [input-file {:keys [diff 
                                              opt-bgcol-change
                                              opt-bgcol-delete
                                              opt-bgcol-insert
                                              opt-catch opt-false
                                              opt-true output]}]
  (with-bindings {#'nassi.transform/*gen-options* 
                  {:true opt-true
                   :false opt-false
                   :catch opt-catch
                   :diff-change-bg opt-bgcol-change
                   :diff-insert-bg opt-bgcol-insert
                   :diff-delete-bg opt-bgcol-delete}}
    (let [html (if diff
                 (diff/to-html diff input-file)
                 (xf/to-html (p/parse-diagram input-file)))]
      (if output
        (spit output html)
        (println html)))))

(defn -main [& args]
  (let [{:keys [input-file options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (generate-html-file input-file options))))

#_(try 
    (generate-html-file "/home/jan/repos/phoenixreisen/phxauth/doc/UC-001_AuthentifizierungEinerBuchung.nassi"      
      {:output "/home/jan/repos/phoenixreisen/nassi/t.html"
       :inline-css true
       :opt-true "Ja"
       :opt-false "Nein"
       :opt-catch "Behandlung der Ausnahmen"
       })
    (catch Exception x (.printStackTrace x)))

#_(try 
    (generate-html-file (io/resource "diff/change-throw-b.nassi")
      {:diff (io/resource "diff/change-throw-a.nassi") ;; original
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
