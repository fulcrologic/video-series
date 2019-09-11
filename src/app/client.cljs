(ns app.client
  (:require
    [app.math :as math]
    ["react-number-format" :as NumberFormat]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as target]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li button h3 label a input table tr td th thead tbody tfoot]]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [app.model.item :as item]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(def ui-number-format (interop/react-factory NumberFormat))

(defn ui-money-input
  "Render a money input component. Props can contain:

  :value - The current controlled value (as a bigdecimal)
  :onChange - A (fn [bigdec]) that is called on changes"
  [{:keys [value
           onChange]}]
  (let [attrs {:thousandSeparator true
               :prefix            "$"
               :value             (math/bigdec->str value)
               :onValueChange     (fn [v]
                                    (let [str-value (.-value v)]
                                      (when onChange
                                        (onChange (math/bigdecimal str-value)))))}]
    (ui-number-format attrs)))

(defsc ItemListItem [this {:item/keys [id title in-stock price] :as props}]
  {:query       [:item/id :item/title :item/in-stock :item/price fs/form-config-join]
   :form-fields #{:item/title :item/in-stock :item/price}
   :ident       :item/id}
  (tr
    (td
      (input {:value    title
              :onChange (fn [evt] (m/set-string! this :item/title :event evt))})
      (div :.ui.left.pointing.red.basic.label
        {:classes [(when (seq title) "hidden")]} "Title must not be empty."))
    (td
      (input {:value    in-stock
              :type     "number"
              :onChange (fn [evt] (m/set-integer! this :item/in-stock :event evt))})
      (div :.ui.left.pointing.red.basic.label
        {:classes [(when (>= in-stock 0) "hidden")]}
        "Quantitiy must be 0 or more."))
    (td
      (ui-money-input {:value    price
                       :onChange (fn [v] (comp/transact! this [(item/set-item-price {:item/id id :item/price v})]))})
      (div :.ui.left.pointing.red.basic.label
        {:classes [(when (math/positive? price) "hidden")]}
        "Price must be a positive value"))))

(def ui-item-list-item (comp/factory ItemListItem {:keyfn :item/id}))

(defmutation add-new-item [{:item/keys [id]}]
  (action [{:keys [state]}]
    (swap! state (fn [s]
                   (-> s
                     (assoc-in [:item/id id] {:item/id id})
                     (target/integrate-ident* [:item/id id]
                       :append [:component/id ::item-list :item-list/all-items]))))))

(defsc ItemList [this {:item-list/keys [all-items] :as props}]
  {:query         [{:item-list/all-items (comp/get-query ItemListItem)}]
   :initial-state {:item-list/all-items []}
   :ident         (fn [] [:component/id ::item-list])}
  (table :.ui.table
    (thead (tr (th "Title") (th "# In Stock") (th "Price")))
    (tbody (map ui-item-list-item all-items))
    (tfoot (tr (th {:colSpan 3}
                 (button :.ui.primary.icon.button
                   {:onClick (fn []
                               (comp/transact! this [(add-new-item {:item/id (random-uuid)})]))}
                   (dom/i :.plus.icon)))))))

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

(defn item-valid?
  "A user-written item validator (by field)"
  [{:item/keys [title in-stock price] :as item} field]
  (case field
    :item/title (boolean (seq title))
    :item/price (math/> price 0)
    :item/in-stock (math/>= in-stock 0)
    false))

(def item-validator (fs/make-validator item-valid?))

(comment

  (item-validator {:item/title "" ::fs/config {::fs/fields    #{:item/title}
                                               ::fs/complete? #{}}} :item/title)

  (let [idt [:item/id 22]]
    (as-> {} s
      ;; Put the form on the screen
      (merge/merge-component s ItemListItem {:item/id 22 :item/title "Original" :item/in-stock 22 :item/price (math/bigdecimal "22.99")})
      (fs/add-form-config* s ItemListItem idt)

      ;; Mutations of some sort update state database
      (assoc-in s [:item/id 22 :item/title] "New Title")
      (assoc-in s [:item/id 22 :item/in-stock] 10)

      ;; 3 POSSIBLE VALIDATION STATUS: :valid :invalid :unchecked
      (fs/mark-complete* s idt :item/title)

      ;; FULCRO RENDERS, sends UI some props.
      (fdn/db->tree (comp/get-query ItemListItem) (get-in s idt) s)

      ;; In UI, using props:

      (fs/dirty-fields s false)

      ))

  )


