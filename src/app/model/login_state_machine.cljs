(ns app.model.login-state-machine
  (:require
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
    [app.routing :as routing]))

(defstatemachine login-machine
  {::uism/aliases
   {:logged-in? [:actor/current-user :user/valid?]}

   ::uism/states
   {:initial
    {::uism/handler (fn [env]
                      (-> env
                        (uism/load :session/current-user :actor/current-user
                          {::uism/ok-event    :event/session-checked
                           ::uism/error-event :event/session-checked})
                        (uism/activate :state/checking-session)))}

    :state/checking-session
    {::uism/events
     {:event/session-checked
      {:uism/handler (fn [env]
                       (let [ok? (uism/alias-value env :logged-in?)]
                         (if ok?
                           (uism/activate env :state/logged-in)
                           (do
                             (routing/route-to! "/login")
                             (-> env
                               (uism/activate :state/entering-credentials))))))}}}

    :state/entering-credentials
    {}

    :state/logged-in
    {}

    :state/logged-out
    {}

    }})
