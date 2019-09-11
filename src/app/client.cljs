(ns app.client
  (:require
    [app.math :as math]
    ["react-number-format" :as NumberFormat]
    [com.wsscode.pathom.core :as p]
    [com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown :refer [ui-dropdown]]
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
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [clojure.set :as set]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.rendering.keyframe-render :as kr]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]))

(def ui-number-format (interop/react-factory NumberFormat))

(defn ui-money-input
  "Render a money input component. Props can contain:

  :value - The current controlled value (as a bigdecimal)
  :onChange - A (fn [bigdec]) that is called on changes"
  [{:keys [value
           onBlur
           onChange]}]
  (let [attrs {:thousandSeparator true
               :prefix            "$"
               :value             (math/bigdec->str value)
               :onBlur            (fn [] (when onBlur (onBlur)))
               :onValueChange     (fn [v]
                                    (let [str-value (.-value v)]
                                      (when (and (seq str-value) onChange)
                                        (onChange (math/bigdecimal str-value)))))}]
    (ui-number-format attrs)))

(defsc ItemCategory [this {:keys [:category/id] :as props}]
  {:query       [:category/id
                 fs/form-config-join]
   :form-fields #{:category/id}
   :ident       :category/id})

(defn table-cell-field [this field {:keys [onChange validation-message input-tag value-xform type]}]
  (let [props         (comp/props this)
        value         (get props field "")
        input-factory (or input-tag dom/input)
        xform         (or value-xform identity)]
    (td
      (input-factory (cond-> {:value    (xform value)
                              :onBlur   (fn [] (comp/transact! this [(fs/mark-complete! {:field field}) :item-list/all-items]))
                              :onChange (fn [evt] (when onChange
                                                    (onChange evt)))}
                       type (assoc :type type)))
      (div :.ui.left.pointing.red.basic.label
        {:classes [(when (not= :invalid (item/item-validator props field)) "hidden")]}
        (or validation-message "Invalid value")))))

(defsc ItemListItem [this {:ui/keys   [new? saving?]
                           :item/keys [id category]
                           :as        props}]
  {:query       [:ui/new?
                 :ui/saving?
                 :item/id :item/title :item/in-stock :item/price
                 {:item/category (comp/get-query ItemCategory)}
                 [:category/options '_]
                 fs/form-config-join]
   :form-fields #{:item/title :item/in-stock :item/price :item/category}
   :pre-merge   (fn [{:keys [data-tree]}] (fs/add-form-config ItemListItem data-tree))
   :ident       :item/id}
  (let [category-options (get props :category/options)]
    (tr
      (table-cell-field this :item/title {:validation-message "Title must not be empty"
                                          :onChange           #(m/set-string! this :item/title :event %)})
      (td
        (ui-dropdown {:options  category-options
                      :search   true
                      :onChange (fn [evt data]
                                  (comp/transact! this [(fs/mark-complete! {:field :item/title})
                                                        :item-list/all-items])
                                  (m/set-value! this :item/category [:category/id (.-value data)]))
                      :value    (:category/id category)}))
      (table-cell-field this :item/in-stock {:validation-message "Quantity must be 0 or more."
                                             :type               "number"
                                             :onChange           #(m/set-integer! this :item/in-stock :event %)})
      (table-cell-field this :item/price {:validation-message "Price must be a positive amount."
                                          :input-tag          ui-money-input
                                          :onChange           #(m/set-value! this :item/price %)})

      (td
        (let [visible? (or new? (fs/dirty? props))]
          (when visible?
            (div :.ui.buttons
              (button :.ui.inline.primary.button
                {:classes  [(when saving? "loading")]
                 :disabled (= :invalid (item/item-validator props))
                 :onClick  (fn []
                             (let [diff (fs/dirty-fields props false {:new-entity? new?})]
                               (comp/transact! this [(item/try-save-item {:item/id id :diff diff})])))} "Save")
              (button :.ui.inline.secondary.button
                {:onClick (fn [] (if new?
                                   (comp/transact! this [(item/remove-item {:item/id id})])
                                   (comp/transact! this [(fs/reset-form! {})])))}
                "Undo"))))))))

(def ui-item-list-item (comp/factory ItemListItem {:keyfn :item/id}))

(defsc ItemList [this {:item-list/keys [all-items] :as props}]
  {:query         [{:item-list/all-items (comp/get-query ItemListItem)}]
   :initial-state {:item-list/all-items []}
   :ident         (fn [] [:component/id ::item-list])}
  (table :.ui.table
    (thead (tr (th "Title") (th "Category") (th "# In Stock") (th "Price") (th "Row Action")))
    (tbody (map ui-item-list-item all-items))
    (tfoot (tr (th {:colSpan 5}
                 (button :.ui.primary.icon.button
                   {:onClick (fn []
                               #_(comp/transact! this [(item/add-new-item {:item/id (tempid/tempid)})])
                               ;; OR
                               (merge/merge-component! this ItemListItem
                                 {:ui/new?       true
                                  :item/id       (tempid/tempid)
                                  :item/title    ""
                                  :item/in-stock 0
                                  :item/price    (math/bigdecimal "0")}
                                 :append [:component/id :app.client/item-list :item-list/all-items]))}
                   (dom/i :.plus.icon)))))))

(def ui-item-list (comp/factory ItemList {:keyfn :item-list/all-items}))

(defsc Root [_ {:root/keys [item-list]}]
  {:query         [{:root/item-list (comp/get-query ItemList)}]
   :initial-state {:root/item-list {}}}

  (div :.ui.container.segment
    (h3 "Inventory Items")
    (ui-item-list item-list)))

(defsc Category [_ _]
  {:query [:category/id :category/name]
   :ident :category/id})

(defn has-reader-error? [v]
  (cond
    (keyword? v) (= v ::p/reader-error)
    (vector? v) (boolean (some has-reader-error? v))
    (map? v) (boolean
               (or
                 (some has-reader-error? (vals v))
                 (some has-reader-error? (keys v))))
    :else false))

(defonce APP (app/fulcro-app {:remotes          {:remote (http/fulcro-http-remote {})}
                              :remote-error?    (fn [{:keys [status-code body]}]
                                                  (or
                                                    #_(has-reader-error? body)
                                                    (not= 200 status-code)))
                              :client-did-mount (fn [app]
                                                  (df/load app :item/all-items
                                                    ItemListItem
                                                    {:target [:component/id ::item-list :item-list/all-items]})
                                                  (df/load app :category/all-categories
                                                    Category
                                                    {:post-mutation `item/create-category-options}))}))

(defn ^:export init []
  (app/mount! APP Root "app"))

