(ns nassi.stepper-test
  (:require 
    [clojure.test :refer :all]
    [nassi.stepper :refer :all]))

(deftest stepper-test 
  (testing "stepper FNs"
    (let [x (create)]
      (is (= "1" (inc-step! x)))
      (is (= "2" (inc-step! x)))
      (add-step! x)
      (is (= "2.1" (inc-step! x)))
      (is (= "2.2" (inc-step! x)))
      (add-step! x)
      (is (= "2.2.1" (inc-step! x)))
      (remove-step! x)
      (is (= "2.3" (inc-step! x)))
      (is (not (contains-step? x :two-three)))
      (pin-step! x :two-three)
      (is (contains-step? x :two-three))
      (is (= "2.4" (inc-step! x)))
      (add-step! x)
      (disable! x "Preamble")
      (is (= "Preamble" (inc-step! x)))
      (enable! x)
      (is (= "2.4.1" (inc-step! x)))
      (fetch-step! x :two-three)
      (is (= "2.4" (inc-step! x)))
      (remove-step! x)
      (is (= "3" (inc-step! x)))
      (pop-step! x)
      (is (= "2.4.2" (inc-step! x))))))
