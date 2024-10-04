(ns nassi.main
  (:require 
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.cli :as cli]
    [nassi.core :as core])
  (:gen-class))

(def ^:private cli-options
  [["-o" "--output <file>" "Set output file name."]
   [nil "--[no-]inline-css" "Include CSS in HTML." :default true]
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

(defn- generate-html-with-inline-css [html]
  (str/join \newline
    ["<!DOCTYPE html>" "<html>" "<head>" "<style>" 
     (slurp (io/resource "diagram.css"))
     "</style>" "</head>" "<body>" html "</body>" "</html>"]))

(defn- generate-html-with-extern-css [html]
  (str/join \newline
    ["<!DOCTYPE html>" "<html>" "<head>" 
     "<link rel=\"stylesheet\" type=\"text/css\" href=\"diagram.css\">" 
     "</head>" "<body>" html "</body>" "</html>"]))

(defn- generate-html-file [input-file {:keys [inline-css output]}]
  (let [diagram (core/to-html input-file)
        html (if inline-css 
               (generate-html-with-inline-css diagram)
               (generate-html-with-extern-css diagram))]
    (if output
      (spit output html)
      (println html))))

(defn -main [& args]
  (let [{:keys [input-file options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (generate-html-file input-file options))))

#_(generate-html-file (io/resource "GutenMorgen2.uc") 
    {:inline-css true
     :output "/home/jan/repos/phoenixreisen/nassi/t.html"})

#_(generate-html-file (io/resource "test/throw1.uc")
    {:inline-css true
     :output "/home/jan/repos/phoenixreisen/nassi/t.html"})

#_(generate-html-file "/home/jan/repos/phoenixreisen/phxauth/doc/UC-001_Login-mit-BN-und-Nachname.uc"
    {:inline-css true
     :output "/home/jan/repos/phoenixreisen/nassi/t.html"})
