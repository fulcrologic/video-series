(ns app.model.session
  (:require
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.fulcro.server.api-middleware :refer [augment-response]]
    [taoensso.timbre :as log]))

(def users (atom {1 {:user/id       1
                     :user/email    "foo@bar.com"
                     :user/password "letmein"}}))

(pc/defresolver user-resolver [env {:user/keys [id]}]
  {::pc/input  #{:user/id}
   ::pc/output [:user/email]}
  {:user/id    id
   :user/email (get-in @users [id :user/email])})

(pc/defresolver current-user-resolver [env _]
  {::pc/output [{:session/current-user [:user/id]}]}
  (let [{:user/keys [id email]} (log/spy :info (-> env :request :session))]
    (if id
      {:user/email  email
       :user/id     id
       :user/valid? true}
      {:user/id     :nobody
       :user/valid? false})))

(pc/defmutation login [env {:user/keys [email password]}]
  {::pc/params #{:user/email :user/password}
   ::pc/output [:user/id :user/valid?]}
  (log/info "Login " email)
  (let [subject (first (filter (fn [u] (= (:user/email u) email)) (vals @users)))]
    (log/spy :info subject)
    (if (and subject (= password (:user/password subject)))
      (augment-response
        {:user/email  email
         :user/id     (:user/id subject)
         :user/valid? true}
        (fn [ring-resp] (assoc ring-resp :session subject)))
      {:user/valid? false})))

(def resolvers [user-resolver current-user-resolver login])
