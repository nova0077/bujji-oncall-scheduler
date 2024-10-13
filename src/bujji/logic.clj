(ns bujji.logic
  (:require [bujji.models :as bjm]))


(defn get-permutations
  [l]
  (cond (= (count l) 1)
        (list l)
        :else
        (apply concat
               (map (fn [head]
                      (map (fn [tail]
                             (cons head tail))
                           (get-permutations (remove #{head} l))))
                    l))))


(defn get-team-wise-users
  "Fetches the user list for each team from the database."
  [teams org-id]
  (reduce
   (fn [team-user-map team-id]
     (assoc team-user-map
            (keyword team-id)
            (get (get (bjm/fetch-by-id "teams"
                                       team-id)
                      "users")
                 org-id)))
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
 

(defn sort-users-by-future-leaves
  ;; Sort users by the number of future leaves in descending order
  [users-list week-num user-leaves]
  (let [user-count-pairs (map (fn [user-id]
                                (let [leaves ((keyword user-id) user-leaves)
                                      future-leaves-cnt (if (seq leaves)
                                                          (reduce (fn [acc leave-num]
                                                                    (if (and (seq leave-num)
                                                                             (> (Integer/parseInt (str leave-num))
                                                                                (Integer/parseInt (str (name week-num)))))
                                                                      (inc acc)
                                                                      acc))
                                                                  0
                                                                  leaves)
                                                          0)]
                                  [user-id future-leaves-cnt]))
                              users-list)
        sorted-users (sort-by second > user-count-pairs)]
    (map first sorted-users)))


(defn get-oncall-member
  "Selects the most appropriate user for on-call duty based on their current on-call count."
  [week-id available-users user-oncall-cnt user-leaves-map
   & {:keys [except-id]
      :or {except-id nil}}]
  (let [users-data (reduce
                    (fn [acc user-id]
                      (let [oncall-cnt (-> (get @user-oncall-cnt
                                                user-id "0")
                                           str
                                           keyword)]
                        (if (= user-id except-id)
                          acc
                          (assoc acc
                                 oncall-cnt
                                 (conj (oncall-cnt acc) user-id)))))
                    {}
                    available-users)
        users-with-less-oncall-cnt (-> (reduce-kv (fn [acc cnt users]
                                                    (cond
                                                      (nil? (seq acc))
                                                      {cnt users}

                                                      (< (Integer/parseInt (name cnt))
                                                         (Integer/parseInt (name (first (keys acc)))))
                                                      {cnt users}

                                                      :else
                                                      acc))
                                                  {}
                                                  users-data)
                                       vals
                                       first)
        sorted-users (sort-users-by-future-leaves users-with-less-oncall-cnt
                                                  week-id
                                                  user-leaves-map)]
    (first sorted-users)))


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


(defn reschedule-oncall
  "Attempts to reschedule on-call users when availability issues arise."
  [week-num users team-oncall-data user-leaves-map user-oncall-cnt teams-schedule team-id]
  (doseq [[week-id oncall-data] @team-oncall-data]
    (when (= (week-id teams-schedule)
             team-id)
      (let [p-id (:primary oncall-data)
            p-available? (check-user-availability p-id
                                                  week-num
                                                  user-leaves-map)
            p-replacement (get-oncall-member week-id
                                             (get-available-users users
                                                                  week-id
                                                                  user-leaves-map)
                                             @user-oncall-cnt
                                             user-leaves-map
                                             :except-id p-id)]
        (when (and (week-num @team-oncall-data)
                   p-available?
                   p-replacement)
          (swap! team-oncall-data
                 assoc
                 week-num {key p-id}
                 week-id {:primary p-id})))))
  (week-num @team-oncall-data))


(defn update-oncall-data
  "Updates the team-oncall-data and user-oncall-cnt atoms with primary and
  secondary on-call members."
  [week-num primary-oncall team-oncall-data user-oncall-cnt]
  (swap! team-oncall-data
         assoc week-num {:primary primary-oncall})

  (swap! user-oncall-cnt
         update primary-oncall
         (fnil inc 0))

  {:primary primary-oncall})


(defn assign-members
  "Assigns on-call members to teams for each week, rescheduling when necessary."
  [team-schedule team-user-map user-leaves-map compromise?]
  (let [user-oncall-cnt (atom {})
        team-oncall-data (atom {})]
    (reduce-kv
     (fn [result week-num team-id]
       (let [team-key (keyword team-id)
             users (team-key team-user-map)
             available-users (get-available-users users
                                                  week-num
                                                  user-leaves-map)
             primary-oncall (or (get-oncall-member week-num
                                                   available-users
                                                   user-oncall-cnt
                                                   user-leaves-map)
                                (reschedule-oncall week-num
                                                   users
                                                   team-oncall-data
                                                   user-leaves-map
                                                   user-oncall-cnt
                                                   team-schedule
                                                   team-id))
             all-available-users (get-available-users (keys user-leaves-map)
                                                      week-num
                                                      user-leaves-map)
             compromised-primary-oncall (get-oncall-member week-num
                                                           all-available-users
                                                           user-oncall-cnt
                                                           user-leaves-map)]
         (cond
           (= primary-oncall nil)
           ;; indicates rescheduling is not possible
           ;; option1: assign this week oncall to any other person irrespective to team
           (if compromise?
             (assoc result
                    week-num (update-oncall-data week-num
                                                 compromised-primary-oncall
                                                 team-oncall-data
                                                 user-oncall-cnt))
             (assoc result
                    :success false))

           :else
           (assoc result
                  week-num (update-oncall-data week-num
                                               primary-oncall
                                               team-oncall-data
                                               user-oncall-cnt)))))
     {}
     team-schedule)))


(defn verify-schedule
  [schedule user-leaves-map]
  (reduce-kv
   (fn [acc week-id data]
     (let [p-id (keyword (:primary data))]
       (if (contains? (p-id user-leaves-map)
                           (name week-id))
           (assoc acc :success false)
           acc)))
   {}
   schedule))


(defn try-schedule
  [org-id teams teams-schedule compromise?]
  (let [team-user-map (get-team-wise-users teams
                                           org-id)
        user-leaves-map (get-user-leaves team-user-map)
        schedule-with-uids (assign-members teams-schedule
                                           team-user-map
                                           user-leaves-map
                                           compromise?)]
    (reduce-kv
     (fn [acc week-id oncall-data]
       (let [p-id (:primary oncall-data)
             p-data (if p-id
                          (bjm/fetch-by-id "profiles"
                                             p-id)
                          nil)
             p-username (get p-data "username" "not-found")]
         (assoc acc
                week-id {:primary p-username})))
     {}
     schedule-with-uids)))


(defn build-schedule
  "Creates the on-call schedule for all teams, handling availability and assignment."
  [org-id start-week end-week compromise?]
  (let [org-details (bjm/fetch-by-id "orgs" org-id)
        teams (get org-details "teams")
        team-permutations (get-permutations teams)]
    (reduce (fn [acc team-permutation]
              (let [schedule (try-schedule org-id
                                           team-permutation
                                           (assign-teams team-permutation
                                                         start-week
                                                         end-week)
                                           compromise?)]
                (assoc acc team-permutation schedule)))
            {}
            team-permutations)))


(println (build-schedule "org2" 38 52 false))
