(ns app.model.session
  (:require
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div a]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [app.routing :as routing]
    [taoensso.timbre :as log]))

(declare logout)

(defsc CurrentUser [this {:keys [:user/id :user/email :user/valid?] :as props}]
  {:query         [:user/id :user/email :user/valid?]
   :initial-state {:user/id :nobody :user/valid? false}
   ;; NOTE: NO NORMALIZATION. We want the user to be in this top-level field, not part of the graph
   }
  (dom/div :.item
    (if valid?
      (div :.content
        email ent/nbsp (a {:onClick
                           (fn [] (comp/transact! this [(logout)]))}
                         "Logout"))
      (a {:onClick #(dr/change-route this ["login"])} "Login"))))

(def ui-current-user (comp/factory CurrentUser))

(defn show-login-busy* [state-map tf]
  (assoc-in state-map [:component/id :login :ui/busy?] tf))

(defn show-login-error* [state-map tf]
  (assoc-in state-map [:component/id :login :ui/error?] tf))


(defmutation login [_]
  (action [{:keys [state]}]
    (swap! state show-login-busy* true))
  (error-action [{:keys [state]}]
    (log/error "Error action")
    (swap! state (fn [s]
                   (-> s
                     (show-login-busy* false)
                     (show-login-error* true)))))
  (ok-action [{:keys [state]}]
    (log/info "OK action")
    (let [logged-in? (get-in @state [:session/current-user :user/valid?])]
      (if logged-in?
        (do
          (swap! state (fn [s]
                         (-> s
                           (show-login-busy* false)
                           (show-login-error* false))))
          (routing/route-to! "/home"))
        (swap! state (fn [s]
                       (-> s
                         (show-login-busy* false)
                         (show-login-error* true)))))))
  (refresh [_]
    [:ui/error? :ui/busy?])
  (remote [env]
    (-> env
      (m/returning CurrentUser)
      (m/with-target [:session/current-user]))))

(defmutation logout [_]
  (action [{:keys [state]}]
    (routing/route-to! "/login")
    (swap! state assoc :session/current-user {:user/id :nobody :user/valid? false}))
  (remote [env] true))
