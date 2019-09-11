(ns app.client
  (:require
    [app.math :as math]
    ["react-number-format" :as NumberFormat]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li button h3 label a input table tr td th thead tbody]]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [app.model.item :as item]
    [com.fulcrologic.fulcro.dom.events :as evt]))

(def ui-number-format (interop/react-factory NumberFormat))

(defsc EditableMoneyInput
  "Render a money input component. Props can contain:

  :value - The current controlled value (as a bigdecimal)
  :onChange - A (fn [bigdec]) that is called on changes"
  [this {:keys [value
                onChange]}]
  {:initLocalState (fn [this props] {:editing? false})}
  (let [{:keys [editing?]} (comp/get-state this)
        attrs {:thousandSeparator true
               :prefix            "$"
               :value             (math/bigdec->str value)
               :onDoubleClick     #(comp/set-state! this {:editing? true})
               :onBlur            (fn [] (comp/set-state! this {:editing? false}))
               :onValueChange     (fn [v]
                                    (let [str-value (.-value v)]
                                      (when onChange
                                        (onChange (math/bigdecimal str-value)))))
               :displayType       (if editing? "input" "text")}]
    (ui-number-format attrs)))

(def ui-editable-money-input (comp/factory EditableMoneyInput))

(defsc ItemListItem [this {:item/keys [id title in-stock price] :as props}]
  {:query [:item/id :item/title :item/in-stock :item/price]
   :ident :item/id}
  (tr
    (td title)
    (td (input {:value    in-stock
                :type     "number"
                :onChange (fn [evt]
                            (m/set-integer! this :item/in-stock :event evt))}))
    (td (ui-editable-money-input {:value    price
                                  :onChange (fn [v]
                                              (comp/transact! this [(item/set-item-price {:item/id id :item/price v})]))}))))

(def ui-item-list-item (comp/factory ItemListItem {:keyfn :item/id}))

(defsc ItemList [this {:item-list/keys [all-items] :as props}]
  {:query         [{:item-list/all-items (comp/get-query ItemListItem)}]
   :initial-state {:item-list/all-items []}
   :ident         (fn [] [:component/id ::item-list])}
  (table :.ui.table
    (thead (tr (th "Title") (th "# In Stock") (th "Price")))
    (tbody (map ui-item-list-item all-items))))

(def ui-item-list (comp/factory ItemList {:keyfn :item-list/all-items}))

(defsc Root [_ {:root/keys [item-list]}]
  {:query         [{:root/item-list (comp/get-query ItemList)}]
   :initial-state {:root/item-list {}}}

  (div :.ui.container.segment
    (h3 "Inventory Items")
    (ui-item-list item-list)))

(defonce APP (app/fulcro-app {:remotes          {:remote (http/fulcro-http-remote {})}
                              :client-did-mount (fn [app]
                                                  (df/load app :item/all-items
                                                    ItemListItem
                                                    {:target [:component/id ::item-list :item-list/all-items]}))}))

(defn ^:export init []
  (app/mount! APP Root "app"))

(comment
  (df/load! APP [:person/id 1] PersonDetail))


