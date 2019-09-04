(ns app.server
  (:require
    [app.model.car :as car]
    [app.model.person :as person]
    [clojure.core.async :as async]
    [com.fulcrologic.fulcro.algorithms.do-not-use :as util]
    [com.fulcrologic.fulcro.server.api-middleware :as fmw :refer [not-found-handler wrap-api]]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.core :as p]
    [org.httpkit.server :as http]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.not-modified :refer [wrap-not-modified]]
    [ring.middleware.resource :refer [wrap-resource]]
    [ring.util.response :refer [response file-response resource-response]]
    [taoensso.timbre :as log]
    [clojure.tools.namespace.repl :as tools-ns]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]))

#_(pc/defmutation todo-delete-item [env {:keys [id list-id]}]
    {::pc/params [:list-id :id]
     ::pc/output []}
    (log/info "Deleted item" id)
    (swap! item-db dissoc id)
    {})

#_(pc/defresolver list-resolver [env params]
    {::pc/input  #{:list/id}
     ::pc/output [:list/title {:list/items [:item/id]}]}
    ;; normally you'd pull the person from the db, and satisfy the listed
    ;; outputs. For demo, we just always return the same person details.
    {:list/title "The List"
     :list/items (into []
                   (sort-by :item/label (vals @item-db)))})

(def my-resolvers [car/car-resolver person/person-resolver])

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
