(ns bujji.core
  (:gen-class)
  (:require [bujji.server :as bjs]
            [ring.adapter.jetty :as jetty]
            [taoensso.timbre :as log]))


(defn main []
  (bjs/main))
