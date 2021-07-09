(ns phoenix-test
  (:require [clojure.test :refer [deftest testing is]]
            [integrant.core :as ig]
            [phoenix :as sut]))

(defmethod sut/run-web-server :function [handler _opts]
  handler)

(defmethod sut/stop-web-server :function [_server])

(deftest a-test
  (testing "FIXME, I fail."
    (let [system (sut/start-system (sut/configure {::sut/web-server {:type :function}}))]
      (try
        (let [request {}
              {:keys [status body]} ((get-in system [::sut/web-server :web-server]) request)]
          (is (= 200 status))
          (is (= "Hello, Phoenix!" body)))
        (finally (ig/halt! system))))))

(comment
  (a-test))
