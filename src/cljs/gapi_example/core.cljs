(ns gapi-example.core
  ^:figwheel-always
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
    (:require
      [reagent.core :as r]
      [cljs.core.async :refer [put! chan <! >! buffer]]))

; https://developers.google.com/identity/sign-in/web/reference#gapiauth2googleauth

(enable-console-print!)
(def user (r/atom {}))

(defn load-gapi-auth2 []
  (let [c (chan)]
    (.load js/gapi "auth2" #(go (>! c true)))
    c))

(defn auth-instance []
  (.getAuthInstance js/gapi.auth2))

(defn get-google-token []
  (-> (auth-instance) .-currentUser .get .getAuthResponse .-id_token))

(defn handle-user-change
  [u]
  (let [profile (.getBasicProfile u)]
    (reset! user
            {:name       (if profile (.getName profile) nil)
             :image-url  (if profile (.getImageUrl profile) nil)
             :token      (get-google-token)
             :signed-in? (.isSignedIn u)})))

(defonce _ (go
             (<! (load-gapi-auth2))
             (.init js/gapi.auth2
                    (clj->js {"client_id" "496634047757-mi6fvvd7lajtf6qb02o1du9qa4omcqdo.apps.googleusercontent.com"
                              "scope"     "profile"}))
             (let [current-user (.-currentUser (auth-instance))]
               (.listen current-user handle-user-change))))

(defn app []
  [:div
   (if-not (:signed-in? @user) [:a {:href "#" :on-click #(.signIn (auth-instance))} "Sign in with Google"]
                               [:div
                                [:p
                                 [:strong (:name @user)]
                                 [:br]
                                 [:img {:src (:image-url @user)}]]
                                [:a {:href "#" :on-click #(.signOut (auth-instance))} "Sign Out"]])])

(r/render [app] (.getElementById js/document "app"))


