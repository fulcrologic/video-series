(ns app.model.car
  (:require
    [com.wsscode.pathom.connect :as pc]))

(def cars
  (atom {1 {:car/id    1
            :car/make  "Honda"
            :car/model "Accord"}
         2 {:car/id    2
            :car/make  "Ford"
            :car/model "F-150"}}))

(pc/defresolver car-resolver [env {:car/keys [id]}]
  {::pc/input  #{:car/id}
   ::pc/output [:car/id :car/make :car/model]}
  (get @cars id))

(def resolvers [car-resolver])
