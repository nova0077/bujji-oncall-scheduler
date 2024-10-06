(ns bujji.routes
  (:require [cheshire.core :as json]
            [compojure.core :refer [context defroutes DELETE GET POST PUT]]
            [bujji.handlers.auth :as bjha]
            [bujji.handlers.org :as bjho]
            [bujji.handlers.profile :as bjhp]
            [bujji.handlers.schedule :as bjhs]
            [bujji.handlers.request :as bjhr]
            [bujji.handlers.teams :as bjht]
            [clojure.walk :as ck]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [taoensso.timbre :as log]
            [ring.middleware.json :as rjson]
            [ring.middleware.params :as rparams]
            [ring.middleware.keyword-params :as kparams]
            [ring.util.response :as res]
            [ring.middleware.cors :as rcors]))


;; Defining routes with POST request for login
(defroutes bujji-xhrs
  (GET "/ping" []
    res/response "pong")

  (context "/auth" []
    (POST "/login" {params :query-params}
      (bjha/login params)))

  (context "/org" []
    (POST "/get" {params :params}
      (bjho/get-org params))

    (POST "/create" {params :params}
      (bjho/create-org params))

    (POST "/edit" {params :params}
      (bjho/edit-org params))

    (POST "/delete" {params :params}
      (bjho/delete-org params)))

  (context "/profile" []
    (POST "/get" {params :params}
      (bjhp/get-profile params))

    (POST "/create" {params :params}
      (bjhp/create-profile params))

    (POST "/edit" {params :params}
      (bjhp/edit-profile params))

    (POST "/delete" {params :params}
      (bjhp/delete-profile params)))

  (context "/schedule" []
    (POST "/get" {params :params}
      (bjhs/get-schedule params))

    (POST "/create" {params :params}
      (bjhs/create-schedule params))

    (POST "/swap" {params :params}
      (bjhs/swap-schedule params))

    (POST "/delete" {params :params}
      (bjhs/delete-schedule params))

    (POST "/make" {params :params}
      (bjhs/make-schedule params)))

  (context "/request" []
    (POST "/get" {params :params}
      (bjhr/get-request params))

    (comment (POST "/create" {params :params}
               (bjhr/create-request params))

             (POST "/edit" {params :params}
               (bjhr/edit-request params))

             (POST "/edit" {params :params}
               (bjhr/edit-request params))

             (POST "/get-status" {params :params}
               (bjhr/get-request-status params))))

  (context "/team" []
    (POST "/get" {params :params}
      (bjht/get-team params))

    (POST "/create" {params :params}
      (do
        (log/info "create-team called with params: " params)
        (bjht/create-team params)))

    (POST "/edit" {params :params}
      (bjht/edit-team params))

    (POST "/delete" {params :params}
      (bjht/delete-team params))))


(def app
  (-> bujji-xhrs
      (rjson/wrap-json-body {:keywords? true})
      (rjson/wrap-json-response)
      (rparams/wrap-params)                    ;; Handle query params, form params
      wrap-multipart-params))


;; (json/parse-string  true)
