(ns bujji.handlers.request
  (:require [bujji.models :as bjm]
            [ring.util.response :as rur]))


(defn get-request
  [params]
  (rur/response (bjm/fetch-by-id "requests"
                                 (get params "id"))))
