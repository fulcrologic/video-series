(ns app.model.category
  (:require
    [com.wsscode.pathom.connect :as pc]
    [clojure.set :as set]))

(def categories (atom {1 {:category/id   1
                          :category/name "Tools"}
                       2 {:category/id   2
                          :category/name "Food"}}))

(pc/defresolver category-resolver [env {:category/keys [id]}]
  {::pc/input  #{:category/id}
   ::pc/output [:category/name]}
  (get @categories id))

(pc/defresolver all-categories-resolver [_ _]
  {::pc/output [{:category/all-categories [:category/id]}]}
  {:category/all-categories (->> categories deref vals (sort-by :category/id) vec)})

(pc/defresolver all-categories-options [_ _]
  {::pc/output [{:category/category-options [:text :value]}]}
  {:category/category-options
   (into []
     (map #(set/rename-keys % {:category/id :value :category/name :text}))
     (->> categories deref vals (sort-by :category/name)))})

(pc/defmutation update-category [env {:category/keys [id name]}]
  {::pc/params [:item/id :item/price]
   ::pc/output [:item/id]}
  (when-not (contains? categories id)
    (throw (ex-info "Cannot update missing cagtegory" {:category/id id})))
  (swap! categories assoc-in [id :category/name] name)
  {:category/id id})

(def resolvers [category-resolver all-categories-resolver all-categories-options update-category])
