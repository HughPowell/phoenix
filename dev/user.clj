(ns user
  (:require [clojure.tools.namespace.repl :as repl]
            [integrant.repl :refer [clear go halt prep init reset reset-all]]
            [phoenix :as phoenix]))

(defn refresh []
  (halt)
  (repl/refresh))

(integrant.repl/set-prep! phoenix/configure)

(defn spy [data]
  (tap> data)
  data)