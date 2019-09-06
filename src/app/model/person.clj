(ns app.model.person
  (:require
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]))

(def people
  (atom {1 {:person/id   1
            :person/name "Bob"
            :person/age  22
            :person/cars #{2}}
         2 {:person/id   2
            :person/name "Sally"
            :person/age  26
            :person/cars #{1}}}))

(comment
  (swap! people assoc-in [1 :person/age] 99)
  (swap! people assoc-in [1 :person/name] "Tony")
  (swap! people update 1 dissoc :person/age))

(pc/defresolver person-resolver [env {:person/keys [id]}]
  {::pc/input  #{:person/id}
   ::pc/output [:person/name :person/age {:person/cars [:car/id]}]}
  (let [person (-> @people
                 (get id)
                 (update :person/cars (fn [ids] (mapv
                                                  (fn [id] {:car/id id})
                                                  ids))))]
    person))

(pc/defresolver all-people-resolver [env {:person/keys [id]}]
  {::pc/output [{:all-people [:person/id]}]}
  {:all-people
   (mapv (fn [i] {:person/id i}) (keys @people))})

(pc/defresolver current-system-time [env {:person/keys [id]}]
  {::pc/output [:server/time]}
  {:server/time (java.util.Date.)})

(pc/defmutation make-older [env {:person/keys [id]}]
  {::pc/params [:person/id]
   ::pc/output []}
  (swap! people update-in [id :person/age] inc)
  {:result 42})

(pc/defmutation select-person [env {:person/keys [id]}]
  {::pc/params [:person/id]}
  {:person/id id})

(def resolvers [person-resolver all-people-resolver make-older current-system-time select-person])
