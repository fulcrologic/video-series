(ns app.server
  (:require
    [app.model.car :as car]
    [app.model.person :as person]
    [clojure.core.async :as async]
    [com.fulcrologic.fulcro.server.api-middleware :as fmw :refer [not-found-handler wrap-api]]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.core :as p]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.not-modified :refer [wrap-not-modified]]
    [ring.middleware.resource :refer [wrap-resource]]))

(def my-resolvers [car/resolvers person/resolvers])

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

(def middleware (-> not-found-handler
                  (wrap-api {:uri    "/api"
                             :parser (fn [query] (async/<!! (parser {} query)))})
                  (fmw/wrap-transit-params)
                  (fmw/wrap-transit-response)
                  (wrap-resource "public")
                  wrap-content-type
                  wrap-not-modified))
