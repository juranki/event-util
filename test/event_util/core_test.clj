(ns event-util.core-test
  (:use clojure.test)
  (:require [event-util.core :as e]))

(with-test
  (defn seq-seq [s]
    (let [sink (-> (e/seq->stream s)
                   e/stream->seq-ref)]
      @sink))
  (defn seq-filter-seq [pred s]
    (let [sink (-> (e/seq->stream s)
                   (e/filter pred)
                   e/stream->seq-ref)]
      @sink))
  (defn matching-seqs? [s1 s2]
    (every? (fn [[a b]] (= a b))
            (map vector s1 s2)))
  
  (is (matching-seqs?
       (range 10)
       (seq-seq (range 10))))

  (is (matching-seqs?
       (filter even? (range 10))
       (seq-filter-seq even? (range 10)))))