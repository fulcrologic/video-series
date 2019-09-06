(ns app.client
  (:require
    [app.model.person :refer [make-older select-person picker-path]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li button h3 label a]]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.data-fetch :as df]))

(defsc Car [this {:car/keys [model]}]
  {:query [:car/id :car/model]
   :ident :car/id}
  (div
    "Model " model))

(def ui-car (comp/factory Car {:keyfn :car/id}))

(defsc PersonDetail [this {:person/keys [id name age cars]}]
  {:query [:person/id :person/name :person/age {:person/cars (comp/get-query Car)}]
   :ident :person/id}
  (let [onClick (comp/get-state this :onClick)]
    (div :.ui.segment
      (h3 :.ui.header "Selected Person")
      (when id
        (div :.ui.form
          (div :.field
            (label {:onClick onClick} "Name: ")
            name)
          (div :.field
            (label "Age: ")
            age)
          (button :.ui.button {:onClick (fn []
                                          (comp/transact! this
                                            [(make-older {:person/id id})]
                                            {:refresh [:person-list/people]}))}
            "Make Older")
          (h3 {} "Cars")
          (ul {}
            (map ui-car cars)))))))

(def ui-person-detail (comp/factory PersonDetail {:keyfn :person/id}))

(defsc PersonListItem [this {:person/keys [id name]}]
  {:query [:person/id :person/name]
   :ident :person/id}
  (li :.item
    (a {:href    "#"
        :onClick (fn []
                   (df/load! this [:person/id id] PersonDetail
                     {:target (picker-path :person-picker/selected-person)}))}
      name)))

(def ui-person-list-item (comp/factory PersonListItem {:keyfn :person/id}))

(defsc PersonList [_ {:person-list/keys [people]}]
  {:query         [{:person-list/people (comp/get-query PersonListItem)}]
   :ident         (fn [] [:component/id :person-list])
   :initial-state {:person-list/people []}}
  (div :.ui.segment
    (h3 :.ui.header "People")
    (ul
      (map ui-person-list-item people))))

(def ui-person-list (comp/factory PersonList))

(defsc PersonPicker [this {:person-picker/keys [list selected-person]}]
  {:query         [{:person-picker/list (comp/get-query PersonList)}
                   {:person-picker/selected-person (comp/get-query PersonDetail)}]
   :initial-state {:person-picker/list {}}
   :ident         (fn [] [:component/id :person-picker])}
  (div :.ui.two.column.container.grid
    (div :.column
      (ui-person-list list))
    (div :.column
      (ui-person-detail selected-person))))

(def ui-person-picker (comp/factory PersonPicker {:keyfn :person-picker/people}))

(defsc Root [_ {:root/keys [person-picker]}]
  {:query         [{:root/person-picker (comp/get-query PersonPicker)}]
   :initial-state {:root/person-picker {}}}
  (div :.ui.container.segment
    (h3 "Application")
    (ui-person-picker person-picker)))

(defonce APP (app/fulcro-app {:remotes          {:remote (http/fulcro-http-remote {})}
                              :client-did-mount (fn [app]
                                                  (df/load! app :all-people PersonListItem
                                                    {:target [:component/id :person-list :person-list/people]}))}))

(defn ^:export init []
  (app/mount! APP Root "app"))

(comment
  (df/load! APP [:person/id 1] PersonDetail))
