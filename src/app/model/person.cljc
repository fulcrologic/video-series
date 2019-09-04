(ns app.model.person
  (:require
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]))

(s/def ::id int?)
(s/def ::name string?)
(s/def ::age pos-int?)

(s/def :app.model/person
  (s/keys
    :req [::id ::name ::age]
    :opt [::cars]))

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
   (pc/defresolver person-resolver [env {::keys [id]}]
     {::pc/input  #{::id}
      ::pc/output [::name {::cars [:app.model.car/id]}]}
     (let [person (-> people
                    (get id)
                    (update ::cars (fn [ids] (mapv
                                               (fn [id] {:app.model.car/id id})
                                               ids))))]
       person)))

#?(:clj
   (pc/defresolver all-people-resolver [env {::keys [id]}]
     {::pc/output [{:all-people [::id]}]}
     {:all-people
      (mapv (fn [i] {::id i}) (keys people))}))

#?(:clj
   (pc/defmutation make-older [env {::keys [id]}]
     {::pc/params [::id]
      ::pc/output []}
     {}))


#?(:clj
   (def resolvers [person-resolver all-people-resolver make-older]))
