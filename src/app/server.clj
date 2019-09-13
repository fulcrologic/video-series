(ns app.server
  (:require
    [clojure.core.async :as async]
    [com.fulcrologic.fulcro.server.api-middleware :as fmw :refer [not-found-handler handle-api-request]]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.core :as p]
    [app.model.session :as session]
    [ring.middleware.session :refer [wrap-session]]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.not-modified :refer [wrap-not-modified]]
    [ring.middleware.resource :refer [wrap-resource]]
    [clojure.string :as str]))

(def my-resolvers [session/resolvers])

;; setup for a given connect system
(def parser
  (p/parallel-parser
    {::p/env     {::p/reader                 [p/map-reader
                                              pc/parallel-reader
                                              pc/open-ident-reader]
                  ::pc/mutation-join-globals [:tempids]}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register my-resolvers})
                  (p/post-process-parser-plugin p/elide-not-found)
                  p/error-handler-plugin]}))

(defn all-routes-to-index [handler]
  (fn [{:keys [uri] :as req}]
    (if (or
          (= "/api" uri)
          (str/ends-with? uri ".css")
          (str/ends-with? uri ".map")
          (str/ends-with? uri ".jpg")
          (str/ends-with? uri ".png")
          (str/ends-with? uri ".js"))
      (handler req)
      (handler (assoc req :uri "/index.html")))))

(defn wrap-api
  [handler]
  (let [parser (fn [env query] (async/<!! (parser env query)))]
    (fn [request]
      (if (= "/api" (:uri request))
        (handle-api-request (:transit-params request) (partial parser {:request request}))
        (handler request)))))

(def middleware (-> not-found-handler
                  (wrap-api)
                  (fmw/wrap-transit-params)
                  (fmw/wrap-transit-response)
                  (wrap-session)
                  (wrap-resource "public")
                  wrap-content-type
                  wrap-not-modified
                  (all-routes-to-index)))
