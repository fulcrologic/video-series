(ns user
  (:require [org.httpkit.server :as http]
            [app.server :refer [middleware]]
            [clojure.tools.namespace.repl :as tools-ns]))

(defonce server (atom nil))

(defn start []
  (let [result (http/run-server middleware {:port 3000})]
    (reset! server result)))

(defn stop []
  (when @server
    (@server)))

(defn restart []
  (stop)
  (tools-ns/refresh-all :after 'user/start))
