(ns event-util.core-test
  (:use clojure.test)
  (:require [event-util.core :as e]))

(with-test
  (defn seq-seq [s]
    (let [source (e/seq->stream s)
          sink   (e/stream->seq-ref source)]
      @sink))
  (defn seq-filter-seq [pred s]
    (let [source (e/seq->stream s)
          f      (e/filter pred source)
          sink   (e/stream->seq-ref f)]
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