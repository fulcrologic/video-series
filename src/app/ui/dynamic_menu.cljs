(ns app.ui.dynamic-menu
  (:require
    [app.routing :as r]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown :refer [ui-dropdown]]
    [com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown-menu :refer [ui-dropdown-menu]]
    [com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown-item :refer [ui-dropdown-item]]
    [com.fulcrologic.fulcro.dom :as dom]))

(defn link
  ([title mutation mutation-params]
   {:type            :link
    :label           title
    :mutation        mutation
    :mutation-params mutation-params})
  ([title mutation] (link title mutation {})))

(defn dropdown
  ([title & items]
   {:type  :dropdown
    :label title
    :items (vec items)}))

(defn dropdown-item
  ([title mutation mutation-params]
   {:label           title
    :mutation        mutation
    :mutation-params mutation-params})
  ([title mutation] (dropdown-item title mutation {})))

(defn menu [& items]
  {:dynamic-menu/items (vec items)})


(defsc DynamicMenu [this {:keys [:dynamic-menu/items] :as props}]
  {:query         [:dynamic-menu/items]
   :initial-state {:dynamic-menu/items []}
   :ident         (fn [] [:component/id ::dynamic-menu])}
  (comp/fragment
    (map (fn [{:keys [type label mutation mutation-params items]}]
           (if (= type :link)
             (dom/div :.item {:onClick #(comp/transact! this [(list mutation (or mutation-params {}))])}
               (dom/div :.content label))
             (ui-dropdown {:item true
                           :text label}
               (ui-dropdown-menu {}
                 (map (fn [{:keys [label mutation mutation-params]}]
                        (ui-dropdown-item {:onClick #(comp/transact! this [(list mutation (or mutation-params {}))])}
                          label))
                   items)))))
      items)))

(def ui-dynamic-menu (comp/factory DynamicMenu))

(defmutation set-menu [menu]
  (action [{:keys [state]}]
    (swap! state assoc-in [:component/id ::dynamic-menu] menu))
  (refresh [_]
    [:dynamic-menu/items]))

(defn set-menu! [this menu]
  (comp/transact! this [(set-menu menu)]))

(defn clear-menu! [this]
  (comp/transact! this [(set-menu (menu))]))
