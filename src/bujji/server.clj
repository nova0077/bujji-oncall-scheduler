(ns bujji.server
  (:require [bujji.models :as bjm]
            [bujji.routes :as bjr]
            [ring.adapter.jetty :as jetty]
            [taoensso.timbre :as log]))

(defn main
  []
  (bjm/connect-db)
  (log/info "Server intialisation started")
  (jetty/run-jetty bjr/app {:port 8080
                            :join? false}))
