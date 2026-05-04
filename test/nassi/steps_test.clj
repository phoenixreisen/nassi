(ns nassi.steps-test
  (:require 
    [clojure.edn     :as    edn]
    [clojure.java.io :as    io]
    [clojure.string  :as    str]
    [clojure.test    :refer :all]
    [clojure.walk    :as    w]
    [nassi.parse     :as    p]
    [nassi.steps     :refer :all]
    [nassi.util      :as    u]))

(defn- check-text-step 
  "Checks if the first word of the passed TEXT node matches the `step` defined
  in the context of the node."
  [[_ {:keys [step]} text :as n]]
  (let [[expected-step _] (str/split text #"\s" 2)]
    (is (= expected-step step))
    n))

(defn- check-steps 
  "Checks whether, after processing the submitted file, each TEXT node has been
  assigned the expected step."
  [fname]
  (let [ast (-> (io/resource (str "test/" fname))
                (p/parse-diagram)
                (add-steps))]
    (w/postwalk
      (fn [x] 
        (cond
          (= :TEXT (u/node-type x)) (check-text-step x)
          :else x))
      ast)
    nil))

(deftest add-steps-test
  (testing "FN add-steps"
    (check-steps "steps_test1.nassi")
    (check-steps "steps_test2.nassi")
    (check-steps "steps_test3.nassi")
    (check-steps "steps_test4.nassi")
    (check-steps "steps_test5.nassi")
    (check-steps "steps_test6.nassi")
    (check-steps "steps_test7.nassi")
    (check-steps "steps_test8.nassi")
    (check-steps "steps_test9.nassi")
    (check-steps "steps_test10.nassi")))
