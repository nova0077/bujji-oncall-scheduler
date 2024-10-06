(ns bujji.utils
  (:import [com.google.firebase.auth FirebaseAuth FirebaseToken]))

(defn verify-google-id-token
  "Verifies the Google ID token using Firebase Authentication."
  [google-id-token]
  (try
    (let [firebase-token (.verifyIdToken (FirebaseAuth/getInstance) google-id-token)
          user-id (.getUid firebase-token)
          user-data (.getUser (FirebaseAuth/getInstance) user-id)]
      {:status 200
       :message "Login successful"
       :user-data (assoc user-data
                         :user-id user-id)})
    (catch Exception e
      {:status 400
       :message (.getMessage e)})))
