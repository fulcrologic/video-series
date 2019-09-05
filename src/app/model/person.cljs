(ns app.model.person
  (:require
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]))

(defmutation make-older [{:person/keys [id] :as params}]
  (action [{:keys [state]}]
    (swap! state update-in [:person/id id :person/age] inc))
  (remote [env] true))
