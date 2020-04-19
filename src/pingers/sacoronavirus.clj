(ns pingers.sacoronavirus
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.pprint :as pprint]
   [clojure.tools.logging :as log]
   [clojure.walk :as walk]
   [clojure.stacktrace :as stacktrace])
  (:import
    (org.openqa.selenium By JavascriptExecutor WebDriver)
    (org.openqa.selenium.chrome ChromeDriver ChromeOptions)))

(def config
  {:url #_"https://sacoronavirus.co.za/" "https://coronavirus.datafree.co"
   :xpath/stats "/html/body/div[1]/div[2]/main/div/section/div/div/div[3]/div/div/div/div[2]"})

(defn init-chrome-driver []
  (doseq [[k v] (->> (slurp "resources/selenium.edn") edn/read-string)]
    (System/setProperty (name k) v)))

(def driver (atom  nil))

(defn extract-stats-data [driver xpath]
  (let [found-element (->> driver (.findElement (By/xpath xpath)) (.getText))]
    (->>
     (str/split found-element #"\n")
     (partition-all 2)
     (map (fn [[v k]] [(str/replace k #"\s*" "") v]))
     (into {})
     walk/keywordize-keys)))

(defn fetch-corona-page []
  (try (when-not (nil? @driver) (.close @driver)) (catch Exception e))
  (init-chrome-driver)
  (let [opts (ChromeOptions.)
        _ (.setHeadless opts true)
        d2 (ChromeDriver. opts)
        _ (reset! driver d2)
        js (cast JavascriptExecutor d2)]
    
    (try 
      (-> @driver (.get (:url config)))
      (-> js (.executeScript "window.scrollBy(0, document.body.scrollHeight)" (to-array [""])))
      (Thread/sleep 2000)
      (extract-stats-data @driver (:xpath/stats config))
      (catch Exception e (log/error (with-out-str (stacktrace/print-cause-trace e)))))))

(defn -main [& args]
  (fetch-corona-page)
  
  (log/info 
   "Scraped Data:" 
   (extract-stats-data @driver (:xpath/stats config)))
  
  (.close @driver))

(comment
  (clojure.walk/keywordize-keys {"foo bar" "val"})
  (-main [])
  (def driver (ChromeDriver.))
  
  (fetch-corona-page)
  (println (with-out-str (pprint/pprint (extract-stats-data @driver (:xpath/stats config)))))
  (.close @driver)
  
  )