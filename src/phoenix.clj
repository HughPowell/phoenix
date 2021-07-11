(ns phoenix
  (:require [clojure.java.io :as io]
            [aero.core :as aero]
            [integrant.core :as ig]
            [meta-merge.core :as meta-merge]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as response])
  (:gen-class))

(defn- greet [request]
  (let [name (get-in request [:path-params :name])]
    (-> (str "Hello, " (or name "World") "!")
        response/response
        (response/content-type "text/plain"))))

(def router
  (ring/router
   [["/" {:name ::root
          :get  greet}]
    ["/:name" {:name ::name
               :get  {:parameters {:path [:map [:name string?]]}
                      :handler    greet}}]]))

(def ^:private app (ring/ring-handler router (ring/create-default-handler)))

(defmulti run-web-server (fn [_handler {:keys [type] :as _opts}] type))

(defmethod run-web-server :default [handler opts]
  (jetty/run-jetty handler opts))

(defmulti stop-web-server (fn [{:keys [type] :as _opts}] type))

(defmethod stop-web-server :default [{:keys [web-server]}]
  (.stop web-server))

(defmethod ig/init-key ::web-server [_ opts]
  (assoc opts :web-server (run-web-server app opts)))

(defmethod ig/halt-key! ::web-server [_ opts]
  (stop-web-server opts))

(defmethod aero/reader 'ig/ref
  [_opts _tag value]
  (ig/ref value))

(defmethod aero/reader 'ig/refset
  [_opts _tag value]
  (ig/refset value))

(defn configure
  ([] (configure []))
  ([& variations]
   (apply
    meta-merge/meta-merge
    (aero/read-config (io/resource "config.edn"))
    variations)))

(defn start-system [config]
  (ig/load-namespaces config)
  (-> config
      ig/prep
      ig/init))

(def stop-system ig/halt!)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [system (-> (map aero/read-config args)
                   configure
                   start-system)]
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. ^Runnable (fn [] (stop-system system))))
    (.join (get-in system [:adapter/web-server :web-server]))))

(comment
  (def config (configure))
  (def system (start-system config))
  (stop-system system))