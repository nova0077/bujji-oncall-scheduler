(ns bujji.handlers.org
  (:require [firestore-clj.core :as f]
            [taoensso.timbre :as log]
            [ring.util.response :as res]
            [bujji.models :as bjm]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [cheshire.core :as json]
            [bujji.specs :as bjsp]))


(defn get-org
  "Fetches an organization by its ID from Firestore."
  [params]
  (try
    (let [result (bjm/fetch-by-id "orgs"
                                  (get params "id"))]
      (res/response {:status 200
                     :result result}))
    (catch Exception e
      (log/error "Error in fetching doc: " (.getMessage e))
      (res/response {:status 400
                     :msg "Failed to fetch doc"}))))


(defn create-org
  "Creates an organization with provided params"
  [params]
  (try
    (do
      (bjm/create-doc "orgs" params)
      (res/response {:status 200
                     :msg "Org created successfully"
                     :params params}))
    (catch Exception e
      (res/response {:status 400
                     :error e}))))


(defn edit-org
  [params]
  (try
    (do
      (bjm/edit-doc "orgs" params)
      (res/response {:status 200
                     :msg "Org updated successfully"
                     :params params}))
    (catch Exception e
      (res/response {:status 400
                     :error e}))))


(defn delete-org
  [params]
  (try
    (do
      (bjm/delete-doc "orgs" params)
      (res/response {:status 200
                     :msg "Org deleted successfully"
                     :params params}))
    (catch Exception e
      (res/response {:status 400
                     :error e}))))


(comment (context "/profile" []
                  (GET "/:id" {params :form-params :keys [ctx]}
                       (bjhp/get-profile params ctx))
                  (POST "/edit/:id" {params :form-params :keys [ctx]}
                        (bjhp/edit-profile params ctx)))

         (context "/request" []
                  (GET "/:id" {params :form-params :keys [ctx]}
                       (bjhr/get-request params ctx))
                  (POST "/create" {params :form-params :keys [ctx]}
                        (bjhr/create-request params ctx))
                  (POST "/edit/:id" {params :form-params :keys [ctx]}
                        (bjhr/edit-request params ctx)))

         (context "/schedule" []
                  (GET "/:id" {params :form-params :keys [ctx]}
                       (bjhs/get-schedule params ctx))
                  (POST "/create" {params :form-params :keys [ctx]}
                        (bjhs/create-schedule params ctx))
                  (POST "/edit/:id" {params :form-params :keys [ctx]}
                        (bjhs/edit-schedule params ctx))))




(comment (POST "/create" {params :form-params :keys [ctx]}
               (bjho/create-org params ctx))
             (POST "/edit/:id" {params :form-params :keys [ctx]}
               (bjho/edit-org params ctx)))
