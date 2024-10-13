(ns bujji.specs
  "Uses malli to validate schema"
  (:require [malli.core :as m]
            [malli.dev :as dev]
            [malli.generator :as mg]
            [malli.error :as me]))

(def id-string
  (m/schema [:string {:min 1 :max 16}]))

(def info-string
  (m/schema [:string {:min 4 :max 25}]))

(def msg-string
  (m/schema [:string {:min 0 :max 255}]))

(def valid-weak
  (:and :int (fn [x]
               (and (>= x 1) (<= x 53)))))

(def request-status
  [:enum "pending" "accepted" "rejected" "approved"])

(def role
  [:enum "admin" "manager" "employee"])

(def request-message
  [:map
   [:approval msg-string]
   [:request msg-string]
   [:response msg-string]])

(def profile-schema
  [:map
   ["id" id-string]
   ["username" info-string]
   ["email" string?]
   ["org_id" id-string]
   ["role" role]
   ["team_id" id-string]
   ["leaves" {:optional true} [:vector string?]]])

(def org-schema
  [:map
   ["id" id-string]
   ["name" info-string]
   ["curr_schedule" id-string]
   ["teams" [:vector id-string]]
   ["users" [:vector id-string]]])

(def request-schema
  [:map
   ["id" id-string]
   ["status" request-status]
   ["from" id-string]
   ["to" id-string]
   ["messages" request-message]])

(def schedule-schema
  [:map
   ["id" id-string]
   ["curr_oncall" id-string]
   ["weak_num" valid-weak]
   ["start_date" string?]])

(def team-schema
  [:map
   ["id" id-string]
   ["org_id" id-string]
   ["name" info-string]
   ["users" [:map
             id-string [:vector id-string]]]
   ["admins" {:optional true} [:vector id-string]]])


(def schema-map
  {"profiles" profile-schema
   "orgs" org-schema
   "requests" request-schema
   "schedules" schedule-schema
   "teams" team-schema})


(defn validate-schema
  [coll config]
  (try
    (if (m/validate (get schema-map coll) config)
      {:success true}
      {:success false
       :error-msg (-> (get schema-map coll)
                      (m/explain config)
                       me/humanize)})
    (catch Exception e
      {:success false
       :error-msg (str "Exception occurred: " (.getMessage e))})))


(mg/generate profile-schema)

(validate-schema :requests {:abc 12})

