(ns app.routing
  (:require
    [app.client-app :refer [APP]]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [clojure.string :as str]
    [pushy.core :as pushy]))

(defonce history (pushy/pushy
                   (fn [p]
                     (let [route-segments (vec (rest (str/split p "/")))]
                       (log/spy :info route-segments)
                       (dr/change-route APP route-segments)))
                   identity))


(defn start! []
  (pushy/start! history))

(defn route-to!
  "Change routes to the given route-string (e.g. \"/home\"."
  [route-string]
  (pushy/set-token! history route-string))

(defmutation route-to
  "Mutation to go to a specific route"
  [{:keys [route-string]}]
  (action [_]
    (route-to! route-string)))
