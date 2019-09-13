(ns app.client
  (:require
    [goog.events :as events]
    [com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown :refer [ui-dropdown]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as target]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li button h3 label a input table tr td th thead tbody tfoot]]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [app.model.session :as session :refer [CurrentUser ui-current-user]]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [pushy.core :as pushy]
    [taoensso.timbre :as log]
    [clojure.string :as str]))

(defsc LoginForm [this {:ui/keys [email password] :as props}]
  {:query         [:ui/email :ui/password]
   :ident         (fn [] [:component/id :login])
   :route-segment ["login"]
   :initial-state {:ui/email    "foo@bar.com"
                   :ui/password "letmein"}}
  (div :.ui.container.segment
    (dom/div :.ui.form
      (div :.field
        (label "Username")
        (input {:value email :onChange #(m/set-string! this :ui/email :event %)}))
      (div :.field
        (label "Password")
        (input {:type     "password"
                :value    password
                :onChange #(m/set-string! this :ui/password :event %)}))
      (button :.ui.primary.button
        {:onClick #(comp/transact! this [(session/login {:user/email    email
                                                         :user/password password})])}
        "Login"))))

(defsc Home [this props]
  {:query         [:pretend-data]
   :ident         (fn [] [:component/id :home])
   :route-segment ["home"]
   :initial-state {}}
  (dom/div :.ui.container.segment
    (h3 "Home Screen")))

(defsc Settings [this props]
  {:query         [:pretend-data]
   :ident         (fn [] [:component/id :settings])
   :route-segment ["settings"]
   :initial-state {}}
  (dom/div :.ui.container.segment
    (h3 "Settings Screen")))

(defrouter MainRouter [this props]
  {:router-targets [LoginForm Home Settings]})

(def ui-main-router (comp/factory MainRouter))

(defsc Root [_ {:root/keys    [ready? router]
                :session/keys [current-user]}]
  {:query         [:root/ready? {:root/router (comp/get-query MainRouter)}
                   {:session/current-user (comp/get-query CurrentUser)}]
   :initial-state {:root/router {}}}
  (div
    (div :.ui.top.fixed.menu
      (div :.item
        (div :.content "My Cool App"))
      (div :.item
        (div :.content (a {:href "/home"} "Home")))
      (div :.item
        (div :.content (a {:href "/settings"} "Settings")))
      (div :.right.floated.item
        (ui-current-user current-user)))
    (div :.ui.grid {:style {:marginTop "4em"}}
      (if ready?
        (ui-main-router router)
        (div :.ui.loader.active)))))

(defmutation finish-login [_]
  (action [{:keys [state]}]
    (swap! state (fn [s]
                   (-> s
                     (assoc :root/ready? true))))))

(declare APP)

(defonce history (pushy/pushy (fn [p]
                                (let [route-segments (vec (rest (str/split p "/")))]
                                  (dr/change-route APP route-segments)
                                  (js/console.log route-segments))) identity))

(defonce APP (app/fulcro-app {:remotes          {:remote (http/fulcro-http-remote {})}
                              :client-did-mount (fn [app]
                                                  (pushy/start! history)
                                                  (dr/initialize! app)
                                                  (df/load! app :session/current-user CurrentUser
                                                    {:post-mutation `finish-login}))}))

(defn ^:export init []
  (app/mount! APP Root "app"))

