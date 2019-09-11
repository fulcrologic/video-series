(ns app.model.item
  (:require [com.fulcrologic.fulcro.mutations :refer [defmutation]]
            [com.fulcrologic.fulcro.components :as comp]
            [com.fulcrologic.fulcro.algorithms.merge :as merge]
            [clojure.set :as set]
            [app.math :as math]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
            [taoensso.timbre :as log]))

(defn item-valid?
  "A user-written item validator (by field)"
  [{:item/keys [title in-stock price] :as item} field]
  (try
    (case field
      :item/title (boolean (seq title))
      :item/price (math/> price 0)
      :item/in-stock (math/>= in-stock 0)
      true)
    (catch :default _
      false)))

(def item-validator (fs/make-validator item-valid?))

(defmutation remove-item [{:item/keys [id]}]
  (action [{:keys [state]}]
    (swap! state (fn [s]
                   (-> s
                     (merge/remove-ident* [:item/id id] [:component/id :app.client/item-list :item-list/all-items])
                     (update :item/id dissoc id))))))

(defmutation add-new-item [{:item/keys [id]}]
  (action [{:keys [state]}]
    (let [ItemListItem (comp/registry-key->class :app.client/ItemListItem)]
      (swap! state (fn [s]
                     (-> s
                       (merge/merge-component ItemListItem {:ui/new?       true
                                                            :item/id       id
                                                            :item/title    ""
                                                            :item/in-stock 0
                                                            :item/price    (math/bigdecimal "0")}
                         :append [:component/id :app.client/item-list :item-list/all-items])))))))

(defmutation create-category-options [_]
  (action [{:keys [state]}]
    (let [categories (sort-by :category/name (vals (get @state :category/id)))]
      (swap! state assoc :category/options (into []
                                             (map #(set/rename-keys % {:category/id   :value
                                                                       :category/name :text}))
                                             categories)))))

(defmutation save-item
  "Unchecked mutation. Sends the given diff to the server without checking validity. See try-save-item."
  [{:item/keys [id]
    :keys      [diff]
    :as        params}]
  (action [{:keys [app state]}]
    (swap! state assoc-in [:item/id id :ui/saving?] true))
  (remote [env] true)
  (ok-action [{:keys [state]}]
    (swap! state (fn [s]
                   (-> s
                     (update-in [:item/id id] assoc :ui/new? false :ui/saving? false)
                     (fs/entity->pristine* [:item/id id])))))
  (error-action [{:keys [state]}]
    (js/alert "Failed to save item")
    (swap! state (fn [s]
                   (-> s
                     (update-in [:item/id id] assoc :ui/saving? false))))))

(defmutation try-save-item [{:item/keys [id]
                             :keys      [diff]
                             :as        params}]
  (action [{:keys [app state]}]
    (let [state-map       @state
          ident           [:item/id id]
          completed-state (fs/mark-complete* state-map ident)
          item            (get-in completed-state ident)
          ItemListItem    (comp/registry-key->class :app.client/ItemListItem)
          item-props      (fdn/db->tree (comp/get-query ItemListItem) item completed-state)
          valid?          (= :valid (item-validator item-props))]
      (if valid?
        (comp/transact! app [(save-item params)])
        (reset! state completed-state)))))
