(ns user
  (:require [integrant.repl :refer [clear go halt prep init reset reset-all]]
            [phoenix :as phoenix]))

(integrant.repl/set-prep! phoenix/configure)