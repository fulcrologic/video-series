(ns app.client-app
  (:require [com.fulcrologic.fulcro.networking.http-remote :as http]
            [com.fulcrologic.fulcro.application :as app]))

(defonce APP (app/fulcro-app {:remotes {:remote (http/fulcro-http-remote {})}}))
