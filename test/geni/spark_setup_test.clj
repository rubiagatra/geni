(ns geni.spark-setup-test
  (:require
    [clojure.java.io :as io]
    [clojure.string]
    [geni.core :as g :refer [dataframe]]
    [midje.sweet :refer [fact =>]])
  (:import
    [java.io File]
    (org.apache.spark.sql Dataset SparkSession)))

(fact "Test spark session and dataframe"
  @g/spark => #(instance? SparkSession %)
  @g/dataframe => #(instance? Dataset %)
  (-> @g/spark .conf .getAll g/scala-map->map) => #(= (% "spark.master") "local[*]"))

(fact "Test primary key is the product of address, date and seller"
  (-> @dataframe
      (g/limit 100)
      (g/with-column
        "entry_id"
        (g/concat "Address" (g/lit "::") "Date" (g/lit "::") "SellerG"))
      (g/select "entry_id")
      g/distinct
      g/count) => 100)

(defn create-temp-file! [ext]
  (let [temp-dir  (io/file (System/getProperty "java.io.tmpdir"))]
    (.toString (File/createTempFile "temporary" ext temp-dir))))

(defn write-then-read-csv! [dataframe]
  (let [temp-file (create-temp-file! ".csv")]
    (g/write-csv! dataframe temp-file)
    (g/read-csv! @g/spark temp-file)))

(fact "Can read and write csv"
  (let [write-df (-> @dataframe (g/select "Suburb" "SellerG") (g/limit 5))
        read-df  (write-then-read-csv! write-df)]
    (g/collect write-df) => (g/collect read-df)))