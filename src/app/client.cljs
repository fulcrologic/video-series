(ns app.client
  (:require
    ["react-number-format" :as NumberFormat]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li h3 label]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]))

(def ui-number-format (interop/react-factory NumberFormat))

(defsc Car [this {:car/keys [id model] :as props}]
  {:query         [:car/id :car/model]
   :ident         :car/id
   :initial-state {:car/id    :param/id
                   :car/model :param/model}}
  (dom/div
    "Model " model))

(def ui-car (comp/factory Car {:keyfn :car/id}))

(defsc Person [this {:person/keys [id name age cars] :as props}]
  {:query         [:person/id :person/name :person/age {:person/cars (comp/get-query Car)}]
   :ident         :person/id
   :initial-state {:person/id   :param/id
                   :person/name :param/name
                   :person/age  20
                   :person/cars [{:id 40 :model "Leaf"}
                                 {:id 41 :model "Escort"}
                                 {:id 42 :model "Sienna"}]}}
  (let [onClick (comp/get-state this :onClick)]
    (div :.ui.segment
      (div :.ui.form
        (div :.field
          (label {:onClick onClick} "Name: ")
          name)
        (div :.field
          (label "Amount: ")
          (ui-number-format {:value "1100221.33"
                             :thousandSeparator true
                             :prefix            "$"}))
        (div :.field
          (label "Age: ")
          age)
        (h3 {} "Cars")
        (ul {}
          (map ui-car cars))))))

(def ui-person (comp/factory Person {:keyfn :person/id}))

(defsc PersonList [this {:person-list/keys [people] :as props}]
  {:query         [{:person-list/people (comp/get-query Person)}]
   :ident         (fn [_ _] [:component/id ::person-list])
   :initial-state {:person-list/people [{:id 1 :name "Bob"}
                                        {:id 2 :name "Sally"}]}}
  (dom/div
    (h3 "People")
    (map ui-person people)))

(def ui-person-list (comp/factory PersonList))

(defsc Sample [this {:root/keys [people]}]
  {:query         [{:root/people (comp/get-query PersonList)}]
   :initial-state {:root/people {}}}
  (div
    (when people
      (ui-person-list people))))

(defonce APP (app/fulcro-app))

(defn ^:export init []
  (app/mount! APP Sample "app"))

(comment
  (comp/component-options Person)

  (comp/transact! APP [(make-older {:person/id 1})])

  (app/current-state APP)

  (merge/merge-component! APP Person {:person/id 1 :person/age 20})

  (comp/get-initial-state Sample)

  )
