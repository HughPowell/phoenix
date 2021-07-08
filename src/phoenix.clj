(ns phoenix
  (:require [clojure.java.io :as io]
            [aero.core :as aero]
            [integrant.core :as ig]
            [meta-merge.core :as meta-merge])
  (:gen-class))

(defmethod ig/init-key ::greet [_key {:keys [data]}]
  (println (str "Hello, " (or (:name data) "World") "!")))

(defmethod aero/reader 'ig/ref
  [_opts _tag value]
  (ig/ref value))

(defmethod aero/reader 'ig/refset
  [_opts _tag value]
  (ig/refset value))

(defn configure
  ([] (configure []))
  ([variations]
   (->> variations
        (cons (io/resource "config.edn"))
        (map aero/read-config)
        (apply meta-merge/meta-merge))))

(defn start-system [config]
  (ig/load-namespaces config)
  (-> config
      ig/prep
      ig/init))

(def stop-system ig/halt!)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (-> args
      configure
      start-system
      stop-system))
