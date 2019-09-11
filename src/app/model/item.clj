(ns app.model.item
  (:require [com.wsscode.pathom.connect :as pc]
            [taoensso.timbre :as log]
            [com.fulcrologic.fulcro.algorithms.tempid :as tempid]))

(def items (atom {1 {:item/id       1
                     :item/title    "Lettuce"
                     :item/in-stock 3
                     :item/category {:category/id 2}
                     :item/price    2.49M
                     }
                  2 {:item/id       2
                     :item/title    "Wrench"
                     :item/in-stock 9
                     :item/category {:category/id 1}
                     :item/price    23.99M}}))

(defn next-id []
  (inc (reduce max (-> items deref keys))))

(pc/defresolver item-resolver [env {:item/keys [id]}]
  {::pc/input  #{:item/id}
   ::pc/output [:item/title :item/in-stock :item/price {:item/category [:category/id]}]}
  (get @items id))

(pc/defresolver all-items-resolver [_ _]
  {::pc/output [{:item/all-items [:item/id]}]}
  {:item/all-items (->> items deref vals (sort-by :item/id) vec)})

(pc/defmutation save-item [env {:item/keys [id]
                                :keys      [diff]}]
  {::pc/output [:item/id]}
  ;(throw (ex-info "Boo" {}))
  (let [new-values (get diff [:item/id id])
        new?       (tempid/tempid? id)
        real-id    (if new? (next-id) id)
        [_ category-id] (get new-values :item/category)
        new-values (cond-> new-values
                     new? (assoc :item/id real-id)
                     category-id (assoc :item/category {:category/id category-id}))]
    (log/info "Saving " new-values " for item " id)
    (Thread/sleep 500)
    (if new?
      (swap! items assoc real-id new-values)
      (swap! items update real-id merge new-values))
    (cond-> {:item/id real-id}
      new? (assoc :tempids {id real-id}))))

(def resolvers [item-resolver all-items-resolver save-item])

