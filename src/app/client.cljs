(ns app.client
  (:require
    [app.model.person :refer [make-older]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li h3 label]]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
    [com.fulcrologic.fulcro.data-fetch :as df]))

(defsc Car [this {:car/keys [id model] :as props}]
  {:query [:car/id :car/model]
   :ident :car/id}
  (dom/div
    "Model " model))

(def ui-car (comp/factory Car {:keyfn :car/id}))

(defsc Person [this {:person/keys [id name age cars] :as props}]
  {:query [:person/id :person/name :person/age {:person/cars (comp/get-query Car)}]
   :ident :person/id}
  (let [onClick (comp/get-state this :onClick)]
    (div :.ui.segment
      (div :.ui.form
        (div :.field
          (label {:onClick onClick} "Name: ")
          name)
        (div :.field
          (label "Age: ")
          age)
        (dom/button :.ui.button {:onClick (fn []
                                            (comp/transact! this
                                              [(make-older {:person/id id})]
                                              {:refresh [:person-list/people]}))}
          "Make Older")
        (h3 {} "Cars")
        (ul {}
          (map ui-car cars))))))

(def ui-person (comp/factory Person {:keyfn :person/id}))

(defsc PersonList [this {:person-list/keys [people]}]
  {:query         [{:person-list/people (comp/get-query Person)}]
   :ident         (fn [] [:component/id ::person-list])
   :initial-state {:person-list/people []}}
  (let [cnt (reduce
              (fn [c {:person/keys [age]}]
                (if (> age 30)
                  (inc c)
                  c))
              0
              people)]
    (div :.ui.segment
      (h3 :.ui.header "People")
      (div "Over 30: " cnt)
      (dom/ul
        (map ui-person people)))))

(def ui-person-list (comp/factory PersonList))

(defsc Root [this {:root/keys [list]}]
  {:query         [{:root/list (comp/get-query PersonList)}]
   :initial-state {:root/list {}}}
  (dom/div
    (dom/h3 "Application")
    (ui-person-list list)))

(defonce APP (app/fulcro-app {:remotes          {:remote (http/fulcro-http-remote {})}
                              :client-did-mount (fn [app]
                                                  (df/load! app :all-people Person
                                                    {:target [:component/id ::person-list :person-list/people]}))}))

(defn ^:export init []
  (app/mount! APP Root "app"))

(comment
  )
