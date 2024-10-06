(ns bujji.logic
  (:require [bujji.models :as bjm]))


(defn get-team-wise-users
  "Fetches the user list for each team from the database."
  [teams]
  (reduce
   (fn [team-user-map team-id]
     (assoc team-user-map
            (keyword team-id)
            (get (bjm/fetch-by-id "teams"
                                   team-id)
                 "users")))
   {}
   teams))


(defn get-available-users
  "Filters out unavailable users based on their leave schedule for a given week."
  [users week-num user-leaves-map]
  (reduce
   (fn [acc user-id]
     (if (not (contains? ((keyword user-id) user-leaves-map)
                         (name week-num)))
       (conj acc user-id)
       acc))
   []
   users))


(defn get-oncall-member
  "Selects the most appropriate user for on-call duty based on their current on-call count."
  [to-get available-users user-oncall-cnt & [except-id]]
  (let [user-data (reduce
                   (fn [acc user-id]
                     (let [oncall-cnt (get user-oncall-cnt
                                           user-id 0)]
                       (cond
                         (= user-id except-id) acc
                         (= oncall-cnt 0) (assoc acc :0 user-id)
                         (= oncall-cnt 1) (assoc acc :1 user-id)
                         (= oncall-cnt 2) (assoc acc :2 user-id))))
                   {}
                   available-users)]
    (cond
      (:0 user-data) (:0 user-data)
      (:1 user-data) (:1 user-data)
      (= to-get "secondary") (:2 user-data)
      :else nil)))


(defn get-user-leaves
  "Fetches the leave schedules for all users in each team."
  [team-user-map]
  (reduce-kv
   (fn [user-leave-map team-id users]
     (reduce
      (fn [acc user-id]
        (assoc acc
               (keyword user-id)
               (set (get (bjm/fetch-by-id "profiles"
                                          user-id)
                         "leaves"))))
      user-leave-map
      users))
   {}
   team-user-map))


(defn assign-teams
  "Assigns teams to each week in a round-robin fashion."
  [teams start-week end-week]
  (reduce
   (fn [result week-num]
     (assoc result
            (keyword (str week-num))
            (nth teams
                 (mod week-num
                      (count teams)))))
   {}
   (range start-week end-week)))


(defn check-user-availability
  "Checks if a user is available for a given week."
  [user-id week-num user-leaves-map]
  (not (contains? (get user-leaves-map
                       user-id)
                  week-num)))


(defn update-team-oncall
  "Updates the team on-call data with new primary or secondary users for the specified weeks."
  [team-oncall-data week-num week-id key primary secondary]
  (swap! team-oncall-data assoc
         week-num {key primary}
         week-id {:primary primary
                  :secondary secondary}))


(defn evaluate-replacement
  "Evaluates whether a replacement is necessary and updates the team on-call data."
  [team-oncall-data week-num week-id to-key key available? replacement primary secondary]
  (when (and (= to-key key) available? replacement)
    (update-team-oncall team-oncall-data
                        week-num
                        week-id
                        key primary secondary)))


(defn reschedule-oncall
  "Attempts to reschedule on-call users when availability issues arise."
  [to-key week-num users team-oncall-data user-leaves-map user-oncall-cnt]
  (doseq [[week-id oncall-data] @team-oncall-data]
    (let [p-id (:primary oncall-data)
          s-id (:secondary oncall-data)
          p-available? (check-user-availability p-id week-num user-leaves-map)
          s-available? (check-user-availability s-id week-num user-leaves-map)
          p-replacement (get-oncall-member "primary"
                                           (get-available-users users week-id user-leaves-map)
                                           @user-oncall-cnt
                                           p-id)
          s-replacement (get-oncall-member "secondary"
                                           (get-available-users users week-id user-leaves-map)
                                           @user-oncall-cnt
                                           s-id)]
      (when-not (nil? (to-key @team-oncall-data))
        (println "Stopping the process as condition is met.")
        (reduced nil))
      (evaluate-replacement team-oncall-data week-num
                            week-id to-key
                            :primary p-available?
                            p-replacement p-id s-id)
      (evaluate-replacement team-oncall-data week-num
                            week-id to-key
                            :primary p-available?
                            s-replacement p-id s-id)
      (evaluate-replacement team-oncall-data week-num
                            week-id to-key
                            :secondary s-available?
                            s-replacement s-id p-id)
      (evaluate-replacement team-oncall-data week-num
                            week-id to-key
                            :secondary s-available?
                            p-replacement s-id p-id))))


(defn assign-members
  "Assigns on-call members to teams for each week, rescheduling when necessary."
  [team-schedule team-user-map user-leaves-map]
  (let [user-oncall-cnt (atom {})
        team-oncall-data (atom {})]
    (reduce-kv
     (fn [result week-num team-id]
       (let [team-key (keyword team-id)
             users (team-key team-user-map)
             available-users (get-available-users users
                                                  week-num
                                                  user-leaves-map)
             primary-oncall (or (get-oncall-member "primary"
                                                   available-users
                                                   @user-oncall-cnt)
                                (reschedule-oncall :primary
                                                   week-num
                                                   users
                                                   team-oncall-data
                                                   user-leaves-map
                                                   user-oncall-cnt))
             secondary-oncall (or (get-oncall-member "secondary"
                                                     (remove #(= % primary-oncall) available-users)
                                                     @user-oncall-cnt)
                                  (reschedule-oncall :secondary
                                                     week-num
                                                     users
                                                     team-oncall-data
                                                     user-leaves-map
                                                     user-oncall-cnt))]
         (swap! team-oncall-data assoc week-num {:primary primary-oncall
                                                 :secondary secondary-oncall})
         (swap! user-oncall-cnt
                update primary-oncall
                (fnil inc 0))
         (when-not (= primary-oncall secondary-oncall)
           (swap! user-oncall-cnt
                  update secondary-oncall
                  (fnil inc 0)))
         (assoc result week-num {:primary primary-oncall
                                 :secondary secondary-oncall})))
     {}
     team-schedule)))


(defn verify-schedule
  [schedule user-leaves-map]
  (reduce-kv
   (fn [acc week-id data]
     (let [p-id (keyword (:primary data))
           s-id (keyword (:secondary data))]
       (if (or (contains? (p-id user-leaves-map)
                          (name week-id))
               (contains? (s-id user-leaves-map)
                          (name week-id)))
         (assoc acc :success false)
         acc)))
   {}
   schedule))


(defn build-schedule
  "Creates the on-call schedule for all teams, handling availability and assignment."
  [org-id start-week end-week]
  (let [org-details (bjm/fetch-by-id "orgs"
                                     org-id)
        teams (get org-details
                   "teams")
        team-schedule (assign-teams teams
                                    start-week
                                    end-week)
        team-user-map (get-team-wise-users teams)
        user-leaves-map (get-user-leaves team-user-map)
        schedule-with-uids (assign-members team-schedule
                                           team-user-map
                                           user-leaves-map)]
    (reduce-kv
     (fn [acc week-id oncall-data]
       (let [p-data (bjm/fetch-by-id "profiles"
                                     (:primary oncall-data))
             p-username (get p-data "username")
             s-data (bjm/fetch-by-id "profiles"
                                     (:secondary oncall-data))
             s-username (get s-data "username")]
         (assoc acc
                week-id {:primary p-username
                         :secondary s-username})))
     {}
     schedule-with-uids)))


(comment (println (build-schedule "org1" 38 52)))
