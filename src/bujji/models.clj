(ns bujji.models
  (:require [bujji.specs :as bjs]
            [clojure.walk :as walk]
            [environ.core :refer [env]]
            [taoensso.timbre :as log]
            [firestore-clj.core :as fs]))

(defonce db-conn (atom nil))

(defn connect-db []
  (when (nil? @db-conn)
    (reset! db-conn
            (if-let [credentials-file (:firestore-credentials-file env)]
              (fs/client-with-creds credentials-file)
              (if-let [project-id (:firestore-project-id env)]
                (fs/default-client project-id)
                (throw (Exception.
                        (str "Unable to connect to Firestore DB, neither FIRESTORE_CREDENTIALS_FILE "
                             "nor FIRESTORE_PROJECT_ID env var found")))))))
  @db-conn)


(defn get-ref-id
  "Returns ref-id of the doc"
  [coll id]
  (-> (fs/coll @db-conn coll)
      (fs/filter= "id" id)
      fs/pullv-with-ids
      first
      first))


(defn fetch-by-id
  [coll id]
  (->> (get-ref-id coll
                   id)
       (str coll "/")
       (fs/doc @db-conn)
        fs/pull))


(defn create-doc
  [coll config]
  (try
    (let [valid-schema (bjs/validate-schema coll
                                            config)]
      (log/info "schema validation result" valid-schema)
      (if (:success valid-schema)
        (do
          (-> (fs/coll @db-conn coll)
              (fs/add! (assoc config
                              "created_at" (fs/server-timestamp)
                              "updated_at" (fs/server-timestamp))))
          "Doc created successfully")
        valid-schema))
    (catch Exception e
      (log/error "Error creating document: " (.getMessage e))
      {:error "Failed to create document"})))


(defn edit-doc
  [coll config]
  (try
    (let [valid-schema (bjs/validate-schema coll
                                            config)]
      (log/info "Schema validation result" valid-schema)
      (if (:success valid-schema)
        (-> @db-conn
            (fs/doc (str coll "/" (get-ref-id coll
                                              (get config "id"))))
            (#(doseq [[k v] (assoc config
                                   "updated_at" (fs/server-timestamp))]
                (fs/assoc! % k v))))
        valid-schema))
    (catch Exception e
      (log/error "Error Updating document: " (.getMessage e))
      {:error "Failed to create document"})))


(defn delete-doc
  [coll config]
  (try
    (-> @db-conn
        (fs/doc (str coll "/" (get-ref-id coll
                                          (get config "id"))))
        fs/delete!)))


;; to get all collections
(comment
  (println
   (f/colls db)))

;; To add data into collection
(comment
  (-> (fs/coll @db-conn "orgs")
      (fs/add! {"name"     "account-x"
                "exchange" "bitmex"})))

(comment (fs/doc @db-conn "orgs/6ULEyzEysNBqxzfjK4h9")

         (def temp (fs/doc @db-conn "orgs/6ULEyzEysNBqxzfjK4h9"))

         (comment (get (fs/pull temp) "name")))


;; Update doc
(comment
  (-> (fs/doc @db-conn "orgs/2JWQu8gmn9BNVWGwbX7U")
      (fs/assoc! "nam" "Infitelybeta")))


(comment (-> (fs/coll @db-conn "orgs")
             (fs/filter= "id" "someid123")
             fs/pullv-with-ids
             first
             first))

(comment (-> (fs/coll @db-conn "orgs")
      (fs/filter= "id" "someid123")
      (fs/assoc! "name" "Helpbot")))
