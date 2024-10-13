(ns bujji.handlers.teams
  (:require [bujji.models :as bjm]
            [taoensso.timbre :as log]
            [ring.util.response :as res]))


(defn get-team
  [params]
  (try
    (let [result (bjm/fetch-by-id "teams"
                                  (get params "id"))]
      (res/response {:status 200
                     :result result}))
    (catch Exception e
      (log/error "Error in fetching doc: " (.getMessage e))
      (res/response {:status 400
                     :msg "Failed to fetch doc"}))))


(defn create-team
  [params]
  (try
    (do
      (bjm/create-doc "teams" params)
      (res/response {:status 200
                     :msg "Team created successfully"
                     :params params}))
    (catch Exception e
      (res/response {:status 400
                     :error e}))))


(defn edit-team
  [params]
  (try
    (do
      (bjm/edit-doc "teams" params)
      (res/response {:status 200
                     :msg "Team updated successfully"
                     :params params}))
    (catch Exception e
      (res/response {:status 400
                     :error e}))))


(defn delete-team
  [params]
  (try
    (do
      (bjm/delete-doc "teams" params)
      (res/response {:status 200
                     :msg "Team deleted successfully"
                     :params params}))
    (catch Exception e
      (res/response {:status 400
                     :error e}))))
