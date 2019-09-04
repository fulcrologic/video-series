(ns app.client
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li h3 label]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
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

(defsc PersonList [this {:person-list/keys [people]}]
  {:query         [{:person-list/people (comp/get-query Person)}]
   :ident         (fn [] [:component/id ::person-list])
   :initial-state {:person-list/people [{:id 99 :name "Joe"}]}}
  (dom/ul
    (map ui-person people)))

(def ui-person-list (comp/factory PersonList))

(defsc Sample [this {:root/keys [list]}]
  {:query         [{:root/list (comp/get-query PersonList)}]
   :initial-state {:root/list {}}}
  (dom/div
    (dom/h3 "Application")
    (ui-person-list list)))

(defonce APP (app/fulcro-app))

(defn ^:export init []
  (app/mount! APP Sample "app"))

(defn get-components-that-query-for-a-prop
  [prop]
  (reduce
    (fn [mounted-instances cls]
      (concat mounted-instances
        (comp/class->all APP (comp/registry-key->class cls))))
    []
    (comp/prop->classes APP prop)))

(comment
  (comp/component-options Person)

  (comp/transact! APP [(make-older {:person/id 1})])

  (map
    comp/get-ident
   (get-components-that-query-for-a-prop :person/name))

  (let [state           (app/current-state APP)
        component-query (comp/get-query Person)
        component-ident [:person/id 99]
        starting-entity (get-in state component-ident)]
    (fdn/db->tree component-query starting-entity state)
    )

  (merge/merge-component! APP Person {:person/id 1 :person/age 20})

  (comp/get-initial-state Sample)

  )
