(ns app.model.session
  (:require
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div a]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
    [app.routing :as routing]
    [taoensso.timbre :as log]))

(defn handle-login [{::uism/keys [event-data] :as env}]
  (let [user-class (uism/actor-class env :actor/user)]
    (-> env
      (uism/trigger-remote-mutation :actor/login-form `login
        (merge event-data
          {:com.fulcrologic.fulcro.mutations/returning              user-class
           :com.fulcrologic.fulcro.algorithms.data-targeting/target [:session/current-user]
           ::uism/ok-event                                          :event/ok
           ::uism/error-event                                       :event/error}))
      (uism/activate :state/checking-credentials))))

(def main-events
  {:event/logout {::uism/handler (fn [env]
                                   (routing/route-to! "/login")
                                   (-> env
                                     (uism/trigger-remote-mutation :actor/login `logout {})
                                     (uism/apply-action assoc-in [::session :current-user] {:user/id :nobody :user/valid? false})))}
   :event/login  {::uism/handler handle-login}})

(defstatemachine session-machine
  {::uism/actor-name
   #{:actor/user
     :actor/login-form}

   ::uism/aliases
   {:logged-in? [:actor/user :user/valid?]}

   ::uism/states
   {:initial
    {::uism/handler
     (fn [{::uism/keys [event-data] :as env}]
       (-> env
         (uism/store :config event-data)                    ; save desired path for later routing
         (uism/load :session/current-user :actor/user {::uism/ok-event    :event/ok
                                                       ::uism/error-event :event/error})
         (uism/activate :state/checking-existing-session)))}

    :state/checking-existing-session
    {::uism/events
     {:event/ok    {::uism/handler (fn [env]
                                     (let [logged-in? (uism/alias-value env :logged-in?)]
                                       (when-not logged-in?
                                         (routing/route-to! "/login"))
                                       (uism/activate env :state/idle)))}
      :event/error {::uism/handler (fn [env] (uism/activate env :state/server-failed))}}}

    :state/bad-credentials
    {::uism/events main-events}

    :state/idle
    {::uism/events main-events}

    :state/checking-credentials
    {::uism/events {:event/ok    {::uism/handler (fn [env]
                                                   (let [logged-in? (uism/alias-value env :logged-in?)
                                                         {:keys [desired-path]} (uism/retrieve env :config)]
                                                     (when (and logged-in? desired-path)
                                                       (routing/route-to! desired-path))
                                                     (-> env
                                                       (uism/activate (if logged-in?
                                                                        :state/idle
                                                                        :state/bad-credentials)))))}
                    :event/error {::uism/handler (fn [env] (uism/activate env :state/server-failed))}}}

    :state/server-failed
    {::uism/events main-events}}})

(defsc CurrentUser [this {:keys [:user/id :user/email :user/valid?] :as props}]
  {:query         [:user/id :user/email :user/valid?]
   :initial-state {:user/id :nobody :user/valid? false}
   :ident         (fn [] [::session :current-user])}
  (dom/div :.item
    (if valid?
      (div :.content
        email ent/nbsp (a {:onClick
                           (fn [] (uism/trigger! this ::sessions :event/logout))}
                         "Logout"))
      (a {:onClick #(dr/change-route this ["login"])} "Login"))))

(def ui-current-user (comp/factory CurrentUser))



