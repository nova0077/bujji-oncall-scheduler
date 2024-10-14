(ns bujji.handlers.profile
  (:require [bujji.models :as bjm]
            [ring.util.response :as res]
            [taoensso.timbre :as log]))


(defn get-profile
  [params]
  (try
    (let [result (bjm/fetch-by-id "profiles"
                                  (get params "id"))]
      (res/response {:status 200
                     :msg result}))
    (catch Exception e
      (log/error "Error in fetching profile: " (.getMessage e))
      (res/response {:status 400
                     :msg "Failed to fetch profile"}))))


(defn create-profile
  [params]
  (try
    (let [user-id (get params "id")
          org-id (get params "org_id")
          org-data (bjm/fetch-by-id "orgs"
                                    org-id)
          org-users (vec (get org-data "users"))
          updated-org-data (assoc org-data
                                  "users" (conj org-users
                                                user-id))
          team-id (get params "team_id")
          team-data (bjm/fetch-by-id "teams"
                                     team-id)
          team-all-users (into {} (get team-data
                                       "users"))
          team-users (vec (get team-all-users
                               org-id))
          updated-team-data (assoc team-data
                                   "users"
                                   (assoc team-all-users
                                          org-id (conj team-users
                                                       user-id)))]
      (do (bjm/create-doc "profiles"
                          params)
          (bjm/edit-doc "orgs"
                        updated-org-data)
          (bjm/edit-doc "teams"
                        updated-team-data)
          (res/response {:status 200
                         :msg "Doc created successfully"
                         :data params})))
    (catch Exception e
      (log/error "Error in creating profile: " (.getMessage e))
      (res/response {:status 400
                     :msg "Failed to create profile"
                     :data (.getMessage e)}))))


(defn edit-profile
  [params]
  (try
    (let [user-id (get params "id")
          org-id (get params "org_id")
          team-id (get params "team_id")
          existing-profile-data (bjm/fetch-by-id "profiles"
                                                 user-id)
          existing-org-id (get existing-profile-data
                               "org_id")
          existing-team-id (get existing-profile-data
                                "team_id")
          existing-team-data (bjm/fetch-by-id "teams"
                                              existing-team-id)
          new-team-data (bjm/fetch-by-id "teams"
                                         team-id)
          existing-org-data (bjm/fetch-by-id "orgs"
                                             existing-org-id)
          new-org-data (bjm/fetch-by-id "orgs"
                                        org-id)]
      (do (bjm/edit-doc "profiles"
                        params)

          (when (not= org-id existing-org-id)
            (let [updated-new-org-data (assoc new-org-data
                                              "users" (conj (get new-org-data
                                                                 "users")
                                                            user-id))
                  updated-existing-org-data (assoc existing-org-data
                                                   "users" (->> (get existing-org-data
                                                                     "users")
                                                                (filter #(not= % user-id))
                                                                vec))
                  team-users (get existing-team-data
                                  "users")
                  updated-team-data (assoc existing-team-data
                                           "users" (assoc team-users
                                                          existing-org-id
                                                          (-> (filter #(not= % user-id)
                                                                      (get team-users
                                                                           existing-org-id))
                                                              vec)
                                                          org-id
                                                          (conj (get team-users
                                                                     org-id)
                                                                user-id)))]
              (do (bjm/edit-doc "orgs"
                                updated-new-org-data)
                  (bjm/edit-doc "orgs"
                                updated-existing-org-data)
                  (bjm/edit-doc "teams"
                                updated-team-data))))

          (when (not= team-id existing-team-id)
            (let [updated-new-team-data (assoc new-team-data
                                               "users" (assoc (get new-team-data
                                                                   "users")
                                                              org-id (-> (get new-team-data
                                                                              "users")
                                                                         (get org-id)
                                                                         (conj user-id))))
                  updated-existing-team-data (assoc existing-team-data
                                                    "users" (assoc (get existing-team-data
                                                                        "users")
                                                                   existing-org-id (-> (get existing-team-data
                                                                                            "users")
                                                                                       (get existing-org-id)
                                                                                       (->> (filter #(not= %
                                                                                                           user-id))
                                                                                            vec))))]
              (do (bjm/edit-doc "teams"
                                updated-new-team-data)
                  (bjm/edit-doc "teams"
                                updated-existing-team-data))))
          (res/response {:status 200
                         :msg "Doc updated successfully"})))
    (catch Exception e
      (log/error "Error in Editing profile: " (.getMessage e))
      (res/response {:status 400
                     :msg (.getMessage e)}))))


(defn delete-profile
  [params]
  (try
    (let [user-id (get params "id")
          user-data (bjm/fetch-by-id "profiles"
                                     user-id)
          team-id (get user-data
                       "team_id")
          org-id (get user-data
                      "org_id")
          org-data (bjm/fetch-by-id "orgs"
                                    org-id)
          team-data (bjm/fetch-by-id "teams"
                                     team-id)
          updated-team-data (assoc team-data
                                   "users" (assoc (get team-data
                                                       "users")
                                                  org-id (-> (get team-data
                                                                  "users")
                                                             (get org-id)
                                                             (->> (filter #(not= %
                                                                                 user-id))
                                                                  vec))))
          updated-org-data (assoc org-data
                                  "users" (-> (filter #(not= %
                                                             user-id)
                                                      (get org-data
                                                           "users"))
                                              vec))]
      (do (bjm/edit-doc "orgs"
                        updated-org-data)
          (bjm/edit-doc "teams"
                        updated-team-data)
          (bjm/delete-doc "profiles"
                          params)
          (res/response {:status 200
                         :msg "Doc deleted successfully"})))
    (catch Exception e
      (log/error "Error in Deleting profile: " (.getMessage e))
      (res/response {:status 400
                     :msg (.getMessage e)}))))
