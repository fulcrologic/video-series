(ns app.model.car
  (:require
    [com.wsscode.pathom.connect :as pc]))

#?(:clj
   (def cars
     {1 {::id    1
         ::make  "Honda"
         ::model "Accord"}
      2 {::id    2
         ::make  "Ford"
         ::model "F-150"}}))

#?(:clj
   (pc/defresolver car-resolver [env {:car/keys [id]}]
     {::pc/input  #{::id}
      ::pc/output [::id ::make ::model]}
     (get cars id)))

#?(:clj
   (def resolvers [car-resolver]))
