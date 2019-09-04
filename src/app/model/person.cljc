(ns app.model.person
  (:require
    [com.wsscode.pathom.connect :as pc]))

#?(:clj
   (def people
     {1 {::id   1
         ::name "Bob"
         ::age  22
         ::cars #{2}}
      2 {::id   2
         ::name "Sally"
         ::age  26
         ::cars #{1}}}))

#?(:clj
   (pc/defresolver person-resolver [env {:person/keys [id]}]
     {::pc/input  #{::id}
      ::pc/output [::name {::cars [:app.model.car/id]}]}
     (let [person (-> people
                    (get id)
                    (update ::cars (fn [ids] (mapv
                                               (fn [id] {:app.model.car/id id})
                                               ids))))]
       person)))

#?(:clj
   (def resolvers [person-resolver]))
