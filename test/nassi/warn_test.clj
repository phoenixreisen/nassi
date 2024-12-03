(ns nassi.warn-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [nassi.parse :as p]
            [nassi.warn :refer :all]))

(deftest exception-warnings-test
  (testing "Test FN exception-warnings"
    (let [ast (p/parse-diagram (io/resource "test/throw2.nassi"))
          actual-warnings (exception-warnings ast)
          expected-warnings ["Warning: No handler defined for error-code '#third-error' from line 3." 
                             "Warning: error-code '#fourth-error' is handled in line 8, but there is no corresponding THROW statement with that particular error-code."]]
      (is (= actual-warnings expected-warnings)))))
