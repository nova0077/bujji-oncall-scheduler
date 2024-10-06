(ns bujji.handlers.auth
  (:require [bujji.utils :as utils]
            [ring.util.response :as rur]
            [taoensso.timbre :as log]))

(defn login
  "Handles login via Firebase Authentication with a Google ID token."
  [params]
  (log/info "Login request received with params:" params)
  (if-let [google-id-token (:google-id-token params)]
    (do
      (log/info "Google ID Token found, proceeding with verification.")
      (let [result (utils/verify-google-id-token google-id-token)]
        (rur/response result)))
    (do
      (log/error "Google ID Token missing in request.")
      (rur/response {:status 400
                          :message "Google ID Token is required"}))))
