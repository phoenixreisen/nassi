(ns nassi.core-test
  "In diesem NS fuehren wir einfaches Approval-Testing durch, um die
  Funktionalitaet des Parses zu pruefen."
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [nassi.core :refer :all]))

(defn- approve 
  "Parst die textuelle Diagramm Repraesentation und prueft, ob das Ergebnis die
  Erwartungen erfuellt. 

  Input-Parameter:

  - `input`     Der Dateiname der Eingabedatei. Die Eingabedatei muss eine
  textuelle Repraesentation eines Diagramms enthalten.

  - `expected`  Der Dateiname des erwarteten Ergebnis. Diese Datei muss die
  erwartete Datenstruktur im EDN-Format enthalten.

  Sowohl die Eingabedatei, als auch die Datei mit dem erwarteten Ergebnis
  muessen unterhalb des Projekt-Verzeichnis `resources/test` liegen."
  ([input]
   (approve input (str input ".edn")))
  ([input expected]
   (let [a (parse-diagram (io/resource (str "test/" input)))
         b (with-open [r (io/reader (io/resource (str "test/" expected)))]
  (edn/read (java.io.PushbackReader. r)))]
   (is (= a b) input))))

(deftest approval-test
  (testing "Could not approve:"
    (approve "simple.uc" )
    (approve "for1.uc" )
    (approve "for2.uc" )
    (approve "while1.uc" )
    (approve "until1.uc" )
    (approve "if1.uc" )
    (approve "if2.uc" )
    (approve "switch2.uc")
    (approve "GutenMorgen.uc")))

;(parse-diagram (io/resource (str "test/GutenMorgen.uc")))
