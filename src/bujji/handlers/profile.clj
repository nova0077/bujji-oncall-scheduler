(ns bujji.handlers.profile
  (:require [bujji.models :as bjm]
            [ring.util.response :as res]
            [taoensso.timbre :as log]))


(defn get-profile
  [params]
  (try
    (let [result (bjm/fetch-by-id "profiles"
                                  params)]
      (res/response {:status 200
                     :msg result}))
    (catch Exception e
      (log/error "Error in fetching profile: " (.getMessage e))
      (res/response {:status 400
                     :msg "Failed to fetch profile"}))))


(defn create-profile
  [params]
  (try
    (let [result (bjm/create-doc "profiles"
                                 params)]
      (res/response {:status 200
                     :msg result}))
    (catch Exception e
      (log/error "Error in creating profile: " (.getMessage e))
      (res/response {:status 400
                     :msg "Failed to create profile"}))))


(defn edit-profile
  [params]
  (try
    (let [result (bjm/edit-doc "profiles"
                               params)]
      (res/response {:status 200
                     :msg result}))
    (catch Exception e
      (log/error "Error in Editing profile: " (.getMessage e))
      (res/response {:status 400
                     :msg "Failed to edit profile"}))))


(defn delete-profile
  [params]
  (try
    (let [result (bjm/delete-doc "profiles"
                                 params)]
      (res/response {:status 200
                     :msg result}))
    (catch Exception e
      (log/error "Error in Deleting profile: " (.getMessage e))
      (res/response {:status 400
                     :msg "Failed to delete profile"}))))
