(ns app.model.item
  (:require [com.wsscode.pathom.connect :as pc]))

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

(pc/defresolver item-resolver [env {:item/keys [id]}]
  {::pc/input  #{:item/id}
   ::pc/output [:item/title :item/in-stock :item/price {:item/category [:category/id]}]}
  (get @items id))

(pc/defresolver all-items-resolver [_ _]
  {::pc/output [{:item/all-items [:item/id]}]}
  {:item/all-items (->> items deref vals (sort-by :item/id) vec)})

(pc/defmutation set-item-price [env {:item/keys [id price]}]
  {::pc/params [:item/id :item/price]
   ::pc/output [:item/id]}
  (when-not (decimal? price)
    (throw (ex-info "API INVARIANT VOILATED!" {:item/price "must be decimal"})))
  (swap! items assoc-in [id :item/price] price)
  {:item-id id})

(def resolvers [item-resolver all-items-resolver set-item-price])
