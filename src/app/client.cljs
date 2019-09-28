(ns app.client
  (:require
    [app.client-app :refer [APP]]
    [app.routing :as routing]
    [app.ui.dynamic-menu :as dynamic-menu]
    [com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown :refer [ui-dropdown]]
    [com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown-menu :refer [ui-dropdown-menu]]
    [com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown-item :refer [ui-dropdown-item]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li button h3 label a input table tr td th thead tbody tfoot]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [app.model.session :as session :refer [CurrentUser ui-current-user]]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [app.routing :as r]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]))

(defsc LoginForm [this {:ui/keys [email password] :as props}]
  {:query         [:ui/email :ui/password [::uism/asm-id '_]]
   :ident         (fn [] [:component/id :login])
   :route-segment ["login"]
   :initial-state {:ui/email    "foo@bar.com"
                   :ui/password "letmein"}}
  (let [current-state    (uism/get-active-state this ::session/sessions)
        busy?            (= :state/checking-credentials current-state)
        bad-credentials? (= :state/bad-credentials current-state)
        error?           (= :state/server-failed current-state)]
    (div :.ui.container.segment
      (dom/div :.ui.form {:classes [(when (or bad-credentials? error?) "error")]}
        (div :.field
          (label "Username")
          (input {:value    email
                  :disabled busy?
                  :onChange #(m/set-string! this :ui/email :event %)}))
        (div :.field
          (label "Password")
          (input {:type      "password"
                  :value     password
                  :disabled  busy?
                  :onKeyDown (fn [evt]
                               (when (evt/enter-key? evt)
                                 (uism/trigger! this ::session/sessions :event/login {:user/email    email
                                                                                      :user/password password})))
                  :onChange  #(m/set-string! this :ui/password :event %)}))
        (when bad-credentials?
          (div :.ui.error.message
            (div :.content
              "Invalid Credentials")))
        (when error?
          (div :.ui.error.message
            (div :.content
              "There was a server error. Please try again.")))
        (button :.ui.primary.button
          {:classes [(when busy? "loading")]
           :onClick #(uism/trigger! this ::session/sessions :event/login {:user/email    email
                                                                          :user/password password})}
          "Login")))))

(defsc Home [this props]
  {:query         [:pretend-data]
   :ident         (fn [] [:component/id :home])
   :route-segment ["home"]
   :initial-state {}}
  (dom/div :.ui.container.segment
    (h3 "Home Screen")))

(defsc Settings [this props]
  {:query                [:pretend-data]
   :ident                (fn [] [:component/id :settings])
   :route-segment        ["settings"]
   :componentDidMount    (fn [this]
                           (dynamic-menu/set-menu! this (dynamic-menu/menu
                                                          (dynamic-menu/link "Other" `other))))
   :componentWillUnmount (fn [this] (dynamic-menu/clear-menu! this))
   :initial-state        {}}
  (dom/div :.ui.container.segment
    (h3 "Settings Screen")))

(defrouter MainRouter [_ _] {:router-targets [LoginForm Home Settings]})

(def ui-main-router (comp/factory MainRouter))

(defsc Root [this {:root/keys    [router dynamic-menu]
                   :session/keys [current-user]}]
  {:query         [{:root/router (comp/get-query MainRouter)}
                   [::uism/asm-id ::session/sessions]
                   {:root/dynamic-menu (comp/get-query dynamic-menu/DynamicMenu)}
                   {:session/current-user (comp/get-query CurrentUser)}]
   :initial-state (fn [_]
                    {:root/router          (comp/get-initial-state MainRouter)
                     :session/current-user (comp/get-initial-state CurrentUser)
                     :root/dynamic-menu    (dynamic-menu/menu)})}
  (let [current-state (uism/get-active-state this ::session/sessions)
        ready?        (not (contains? #{:initial :state/checking-existing-session} current-state))
        logged-in?    (:user/valid? (log/spy :info current-user))]
    (div
      (div :.ui.top.fixed.menu
        (div :.item
          (div :.content "My Cool App"))
        (when logged-in?
          (comp/fragment
            (div :.item
              (div :.content (a {:href "/home"} "Home")))
            (div :.item
              (div :.content (a {:href "/settings"} "Settings")))
            (dynamic-menu/ui-dynamic-menu dynamic-menu)))
        (div :.right.floated.item
          (ui-current-user current-user)))
      (when ready?
        (div :.ui.grid {:style {:marginTop "4em"}}
          (ui-main-router router))))))

(defn refresh []
  (app/mount! APP Root "app"))

(defn ^:export start []
  (app/mount! APP Root "app")
  (routing/start!)
  (uism/begin! APP session/session-machine ::session/sessions
    {:actor/user       session/CurrentUser
     :actor/login-form LoginForm}
    {:desired-path (some-> js/window .-location .-pathname)}))

