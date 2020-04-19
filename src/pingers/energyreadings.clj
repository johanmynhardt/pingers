(ns pingers.energyreadings
  (:require
   [clj-time.core :as time]
   [clj-time.format :as time-format]
   [clojure.tools.logging :as log]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [next.jdbc :as jdbc]
   [pingers.ragtime :refer [db-spec]]))

(defn csv-field [headings field row]
  (let [field-idx
        (->> 
         headings
         (map-indexed (fn [idx x] [x idx]))
         (into {}))]
    (nth row (get field-idx field))))

(defn read-csv [resource] (-> resource io/resource io/reader csv/read-csv))

(defn import-csv [resource-location]
  (let [[headings & records] (read-csv resource-location)
        val-for-heading (partial csv-field headings)
        db-time-format (time-format/formatter "yyyy-MM-dd HH:mm:ss.SSS")
        csv-time-format (time-format/formatter "yyyy-MM-dd'T'HH:mm:ss")]
    
    (doseq [row records]
      (let [from #(val-for-heading % row)
            timestamp (from "Timestamp")
            meter-reading (from "Meter Reading")
            notes (as-> (from "Notes") notes (when (seq notes) notes))]
        
        (log/info 
         "Saving entity: "
         {:timestamp timestamp
          :meter-reading meter-reading
          :notes notes})
        
        (let [save-result 
              (jdbc/with-transaction [trans db-spec]
                (jdbc/execute!
                 trans
                 ["insert into energyreading (timestamp, reading) values (?, ?)"
                  (time-format/unparse
                   db-time-format
                   (time/plus (time-format/parse csv-time-format timestamp) (time/hours 2))) meter-reading]))]
          (log/info "save result: " save-result))))))

(comment 
  
  (time-format/unparse
   (time-format/formatter "yyyy-MM-dd HH:mm:ss")
   (time-format/parse (time-format/formatter "yyyy-MM-dd'T'HH:mm:ss") "2020-04-19T18:00:00"))
  
  (def unit27 "csv/dune-crest-27-energy-use.csv")
  (def data (read-csv unit27))
  
  (import-csv unit27)
  
  (jdbc/with-transaction [trans db-spec]
    (jdbc/execute!
     trans 
     ["insert into energyreading (timestamp, reading) values (?, ?)" 
      (time-format/unparse 
       (time-format/formatter-local "yyyy-MM-dd HH:mm:ss.SSS") (time/now)) 22.2]))
  
  (with-open [conn (jdbc/get-connection db-spec)]
    (jdbc/execute! conn ["select * from energyreading"]))
  
  
  (jdbc/with-db-connection [conn db-spec]
    (jdbc/query conn "select * from energyreading"))
  
  (clojure.stacktrace/e))