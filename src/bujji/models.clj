(ns bujji.models
  (:require [bujji.specs :as bjs]
            [clojure.walk :as walk]
            [environ.core :refer [env]]
            [taoensso.timbre :as log]
            [firestore-clj.core :as fs])
  (:import [java.util HashMap ArrayList]))

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


(defn sanitize-params
  "Recursively converts Java arrays and hashmaps to Clojure data types for the input params, preserving string keys."
  [params]
  (letfn [(convert [value]
            (cond
              ;; If it's a java.util.HashMap, convert it to a Clojure map
              (instance? java.util.Map value) (into {} (map (fn [[k v]] [k (convert v)]) value))
              ;; If it's a java.util.List (Array), convert it to a Clojure vector
              (instance? java.util.List value) (vec (map convert value))
              (instance? java.util.ArrayList value) (vec (map convert value))
              :else value))]
    (convert params)))



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
        fs/pull
        sanitize-params))


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
          {:success true
           :msg "Doc created successfully"})
        valid-schema))
    (catch Exception e
      (log/error "Error creating document: " (.getMessage e))
      (throw e))))


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
      (throw e))))


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
