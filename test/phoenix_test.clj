(ns phoenix-test
  (:require [clojure.test :refer [testing is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as generators]
            [clojure.test.check.properties :refer [for-all]]
            [malli.generator :as generator]
            [reitit.core :as reitit]
            [phoenix :as sut]
            [reitit.ring :as ring])
  (:import (java.io InputStream ByteArrayInputStream)))

(defmethod sut/run-web-server :function [handler _opts]
  handler)

(defmethod sut/stop-web-server :function [_server])

(def port-max (+ (Short/MAX_VALUE) (Math/abs ^Integer (Short/MIN_VALUE))))
(def ipv4-schema [:re #"^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$"])
(def ipv6-std-schema [:re #"^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"])
(def ipv6-hex-compressed-schema
  [:re #"^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$"])

(defn request-schema [router route-name method]
  [:map
   [:server-port [int? {:min 0 :max port-max}]]
   [:server-name [:re #"^([A-Za-z0-9][A-Za-z0-9-]{1,62}[A-Za-z0-9]?\.)+[A-Za-z]{2,6}$"]]
   [:remote-addr [:or ipv4-schema ipv6-std-schema ipv6-hex-compressed-schema]]
   [:uri
    [string?
     {:gen/gen
      (generators/let [path-params (-> router
                                       (reitit/match-by-name route-name)
                                       :result
                                       method
                                       (get-in [:data :parameters :path])
                                       generator/generator)]
        (-> router
            (reitit/match-by-name route-name path-params)
            reitit/match->path))}]]
   [:query-string string?]
   [:scheme [:enum :http :https]]
   [:request-method (into [:enum {:gen/elements [method]}] ring/http-methods)]
   [:headers
    [:map-of string? string?]]
   [:body
    [:and
     {:gen/schema [string? {:gen/fmap (fn [s] (ByteArrayInputStream. (.getBytes s)))}]}
     [:fn (fn [body] (instance? InputStream body))]]]])

(defn return-greeting [request]
  (let [system (sut/start-system (sut/configure {::sut/web-server {:type :function}}))
        endpoint (get-in system [::sut/web-server :web-server])]
    (try
      (endpoint request)
      (finally (sut/stop-system system)))))

(defspec getting-the-name-route
  (for-all [request (generator/generator (request-schema sut/router ::sut/name :get))]
           (testing "successfully returns a greeting"
             (let [{:keys [status body]} (return-greeting request)
                   name (subs (:uri request) 1)]
               (is (= 200 status))
               (if (empty? name)
                 (is (= "Hello, World!" body))
                 (is (= (format "Hello, %s!" name) body)))))))

(comment
  (getting-the-name-route))
