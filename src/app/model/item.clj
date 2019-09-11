(ns app.model.item
  (:require [com.wsscode.pathom.connect :as pc]))

(def items (atom {1 {:item/id       1
                     :item/title    "A thing"
                     :item/in-stock 3
                     :item/price    1022.33M}}))

(pc/defresolver item-resolver [env {:item/keys [id]}]
  {::pc/input  #{:item/id}
   ::pc/output [:item/title :item/in-stock :item/price]}
  (get @items id))

(pc/defresolver all-items-resolver [_ _]
  {::pc/output [:item/all-items]}
  {:item/all-items (->> items deref vals (sort-by :item/id) vec)})

(pc/defmutation set-item-price [env {:item/keys [id price]}]
  {::pc/params [:item/id :item/price]
   ::pc/output [:item/id]}
  (when-not (decimal? price)
    (throw (ex-info "API INVARIANT VOILATED!" {:item/price "must be decimal"})))
  (swap! items assoc-in [id :item/price] price)
  {:item-id id})

(def resolvers [item-resolver all-items-resolver set-item-price])
