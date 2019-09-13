(ns app.model.session
  (:require
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div a]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]))

(defsc CurrentUser [this {:keys [:user/id :user/email :user/valid?] :as props}]
  {:query         [:user/id :user/email :user/valid?]
   :initial-state {:user/id :nobody :user/valid? false}
   :ident         :user/id}
  (dom/div :.item
    (if valid?
      (div :.content
        email ent/nbsp (a {} "Logout"))
      (a {:onClick #(dr/change-route this ["login"])} "Login"))))

(def ui-current-user (comp/factory CurrentUser {:keyfn :user/id}))

(defmutation login [_]
  (remote [env]
    (-> env
      (m/returning CurrentUser)
      (m/with-target [:session/current-user]))))
