(ns covid19-mx-time-series.core
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.data.json :as json]))

(defn parse-int [s]
  (Integer/parseInt (re-find #"\A-?\d+" s)))

(defn fetch-daily-states
  []
  (let [map-url "https://ncov.sinave.gob.mx/Mapa.aspx/Grafica22"
        headers {:headers
                 {"Content-Type" "application/json; charset=utf-8"}}
        data (http/post map-url headers)]
    (json/read-str (:d (json/read-json (:body data))))))

(defn check-state-totals
  []
  (apply + (map (comp parse-int #(nth % 7))
                (fetch-daily-states))))

(defn write-daily-states
  [date data]
  (let [existing (slurp "data/states.edn")
        current (if (= existing "") []  (read-string existing))]
    (spit "states.edn" (pr-str
                        (concat current
                                [{:date date :data data}])))))


(defn read-daily-states
  []
  (read-string (slurp "data/states.edn")))


(defn state-name
  [day-value]
  (second day-value))


(defn state-confirmed
  [day-value]
  (nth day-value 4))

(defn state-negatives
  [day-value]
  (nth day-value 5))

(defn state-suspects
  [day-value]
  (nth day-value 6))

(defn state-deaths
  [day-value]
  (nth day-value 7))


(defn make-time-series
  [state-vals value-fn]
  (vec (concat [(state-name (first state-vals))]
               (map value-fn state-vals))))

(defn state-vals
  [daily-states state-pos]
  (doall (map (comp (fn [x] (nth x state-pos)) :data) daily-states)))

(defn write-timeseries-csv
  [daily-states value-fn filename]
  (with-open [writer (io/writer filename)]
    (let [d (map :data daily-states)
          dates (map :date daily-states)
          state-vecs (map #(state-vals daily-states %) (range 32))]
      (csv/write-csv writer
                     (concat [(concat ["Estado"] dates)]
                             (map #(make-time-series % value-fn) state-vecs))))))

(defn write-all-csvs
  []
  (let [ds (read-daily-states)
        valfns [{:valfn state-suspects
                 :file "covid19_suspects_mx.csv"}
                {:valfn state-negatives
                 :file "covid19_negatives_mx.csv"}
                {:valfn state-confirmed
                 :file "covid19_confirmed_mx.csv"}
                {:valfn state-deaths
                 :file "covid19_deaths_mx.csv"}]
        dir "data/"]
    (map #(write-timeseries-csv ds (:valfn %) (str dir (:file %))) valfns)))




(defn -main
  [& args])
