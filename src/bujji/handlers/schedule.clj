(ns bujji.handlers.schedule
 (:require [bujji.models :as bjm]
           [bujji.logic :as bjl]
           [ring.util.response :as res]))


(defn get-schedule
  [params]
  (res/response (bjm/fetch-by-id "schedule"
                                 (get params "id"))))


(defn create-schedule
  [params]
  )


(defn swap-schedule
  [params])


(defn delete-schedule
  [params])


(defn make-schedule
  [params]
  (bjl/build-schedule params)
  {:status 200})
