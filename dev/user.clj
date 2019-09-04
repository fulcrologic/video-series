(ns user
  (:require [org.httpkit.server :as http]
            [app.server :refer [middleware]]
            [clojure.tools.namespace.repl :as tools-ns]
            [taoensso.timbre :as log]))

;; make sure not to find things in places like resources
(tools-ns/set-refresh-dirs "src" "dev")

(defonce server (atom nil))

(defn start []
  (let [result (http/run-server middleware {:port 3000})]
    (log/info "Started web server on port 3000")
    (reset! server result)
    :ok))

(defn stop []
  (when @server
    (log/info "Stopped web server")
    (@server)))

(defn restart []
  (stop)
  (log/info "Reloading code")
  (tools-ns/refresh :after 'user/start))
