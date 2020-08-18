(ns fr24-logger.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [java-time :as jt]
            [me.raynes.fs :as fs]
            [overtone.at-at :as at])
  (:gen-class))

(def FR24-URL "https://data-live.flightradar24.com/zones/fcgi/feed.js?bounds=37.50,33.67,47.76,54.90&faa=1&satellite=1&mlat=1&flarm=1&adsb=1&gnd=1&air=1&vehicles=1&estimated=1&maxage=14400&gliders=1&stats=1")

(def logs-folder "logs")

(def default-log-file-name "nocallsign_")

(def interval-ms 5000)

(defn get-csv-info [fd]
  (let [airline (:airline fd)
        aircraft (:aircraft-type fd)
        icao-addr (:icao-addr fd)
        flight-no (:flight-no fd)
        orig (:origin fd)
        dest (:destination fd)]
    (str "Airline: " airline ", Aircraft: " aircraft ", From: " orig ", To: " dest
         ", Flight No: " flight-no ", ICAO 24-bit Addr: " icao-addr "\n")))

(defn get-csv-header []
  "Unix_Time(sec),Latitude,Longitude,Heading,Altitude,Ground_Speed(kts)\n")

(defn get-log-folder-name []
  (jt/format "YYYY-MM-dd_HH-mm-ss" (jt/local-date-time)))

(defn vect2map [v]
  {:icao-addr (get v 0) ; a 24-bit alphanumeric address
   :lat (get v 1) ; (deg)
   :lon (get v 2) ; (deg)
   :heading (get v 3) ; (deg)
   :alt (get v 4) ; (ft)
   :ground-spd (get v 5) ; (kts)
   :squawk (get v 6)
   :aircraft-type (get v 8)
   :registration (get v 9)
   :time (get v 10) ; Unix Time (sec)
   :origin (get v 11) ; IATA
   :destination (get v 12) ; IATA
   :callsign (get v 13) ; IATA
   :flight-no (get v 16) ; ICAO
   :airline (get v 18) ; ICAO
   })

(def file-name-map (atom {}))
(def unnamed-cntr (atom 0))

(defn assoc-file-name [icao-addr]
  (when-not (contains? @file-name-map icao-addr)
    (do
      (swap! unnamed-cntr inc)
      (swap! file-name-map assoc icao-addr (str default-log-file-name unnamed-cntr)))
    )
  (get file-name-map icao-addr))

(defn get-log-file-name
  "fd: flight data (a single record)"
  [fd]
  (let [file-name ((comp str/trim :flight-no) fd)]
    (if (empty? file-name)
      (assoc-file-name (:icao-addr fd))
      (do
        (swap! file-name-map assoc (:icao-addr fd) file-name)
        file-name))))

(defn map2csv
  "fd: flight data"
  [fd]
  (str
   (str/join "," [(:time fd)
                      (:lat fd)
                      (:lon fd)
                      (:heading fd)
                      (:alt fd)
                  (:ground-spd fd)])
   "\n"))

(defn save-to-file
  "fd: flight data (a single record)"
  [folder fd]
  (let [file-name (get-log-file-name fd)
        file-path (str folder "/" file-name ".csv")]
    (when-not (fs/exists? file-path) ; Condition Needed???
      (do
        (spit file-path (get-csv-info fd))
        (spit file-path (get-csv-header) :append true)))
    (spit file-path (map2csv fd) :append true)))

(defn fetch-flights-info []
  (->> (http/get FR24-URL)
       :body
       json/read-str
       vals
       (filter vector?)
       (map vect2map)
       vec))

(defn process-data [subfolder]
  (let [dataset (fetch-flights-info)]
    (doseq [fd dataset]
      (save-to-file subfolder fd))))

(def my-pool (at/mk-pool))

(defn -main
  [& args]
  (let [log-subfolder (str logs-folder "/" (get-log-folder-name))]
    (do
      (fs/mkdir log-subfolder)
      (at/every interval-ms #(process-data log-subfolder) my-pool))))

(defn stop (at/stop *1))
