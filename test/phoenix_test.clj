(ns phoenix-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as string]
            [phoenix :as phoenix]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= "Hello, World!" (string/trim (with-out-str (phoenix/greet {})))))))
