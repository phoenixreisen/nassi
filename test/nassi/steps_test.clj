(ns nassi.steps-test
  (:require 
    [clojure.test :refer :all]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [nassi.steps :refer :all]))

(deftest process-sentence-node-test
  (testing "process-sentence-node function"
    (let [f #'nassi.steps/process-sentence-node]
      (is (= (f [:SENTENCE " !!x1-2 Bla, bla, bla"] "1.2")
             [:SENTENCE "!!x1-2" "1.2" "Bla, bla, bla"]))
      (is (= (f [:SENTENCE "Bla, bla, bla"] "1.2")
             [:SENTENCE nil "1.2" "Bla, bla, bla"])))))

(defn- p [s & more]
  [:PARAGRAPH (str "\"\"\"" (str/join \newline (cons s more)) "\"\"\"")])

(deftest process-paragraph-node-test
  (testing "process-paragraph-node function"
    (let [f #'nassi.steps/process-paragraph-node]
      (are [x y] (= x y)
        (f (p "!!x1-0") "1.0")
        [:PARAGRAPH "!!x1-0" "1.0" ""]
        
        (f (p "\t !!ID2") "2")
        [:PARAGRAPH "!!ID2" "2" "\t "]

        (f (p "!!x3 Lorem ipsum dolor sit amet" ) "3")
        [:PARAGRAPH "!!x3" "3" "Lorem ipsum dolor sit amet"]

        (f (p "\t!!x4 Lorem ipsum dolor sit amet" ) "4")
        [:PARAGRAPH "!!x4" "4" "\tLorem ipsum dolor sit amet"]

        (f (p "!!ID5"
             "  Lorem ipsum dolor sit amet,"
             "  consectetur adipiscing elit...") 
          "5")
        [:PARAGRAPH "!!ID5" "5" 
         "Lorem ipsum dolor sit amet,\n  consectetur adipiscing elit..."]

        (f (p "\t!!ID6"
             "  Lorem ipsum dolor sit amet,"
             "  consectetur adipiscing elit...") 
          "6")
        [:PARAGRAPH "!!ID6" "6" 
         "\tLorem ipsum dolor sit amet,\n  consectetur adipiscing elit..."]

        (f (p ""
             "  !!ID7"
             "  Lorem ipsum dolor sit amet,"
             "  consectetur adipiscing elit...") 
          "7")
        [:PARAGRAPH "!!ID7" "7" 
         "\n  Lorem ipsum dolor sit amet,\n  consectetur adipiscing elit..."]))))
