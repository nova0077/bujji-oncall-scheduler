(ns bujji.handlers.request
  (:require [bujji.models :as bjm]
            [ring.util.response :as rur]))


(defn get-request
  [id]
  (rur/response (bjm/fetch-by-id "requests"
                                 id)))
