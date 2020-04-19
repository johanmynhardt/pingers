(ns pingers.ragtime
  (:require
   [ragtime.repl]
   [ragtime.jdbc :as jdbc]
   [clojure.tools.logging :as log]
   [clojure.pprint :as pprint]))

(def db-spec
  {:dbtype "mysql"
   :dbname "pingers"
   :user "root"
   :password "example"
   :host "127.0.0.1"})

(def config {:datastore (jdbc/sql-database db-spec)
   
   :migrations (jdbc/load-resources "migrations")})

(defn run-migrate []
  (log/info "Running migration using config:" config)
  (ragtime.repl/migrate config))

(defn rollback-migrate []
  (log/info "Performing rollback using config:" config)
  (ragtime.repl/rollback config))

(defn -main [& [op :as args]]
  (condp = op
    "migrate" (run-migrate)
    "rollback" (rollback-migrate)
    (throw (ex-info "Expected one of migrate or rollback" {:args args}))))
