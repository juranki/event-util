(ns event-util.core-test
  (:use clojure.test)
  (:require [event-util.core :as e]))

(with-test
  (defn delayed-seq->stream [coll t]
    (reify e/EventSource
      (e/subscribe [_ sink]
        (future
          (Thread/sleep t)
          (doseq [elem coll] (e/on-value sink elem))
          (e/on-done sink)))
      (e/unsubscribe [_ sink] true)))

  (defn seq-seq [s]
    (let [sink (-> (e/seq->stream s)
                   e/stream->seq-ref)]
      @sink))
  (defn seq-filter-seq [pred s]
    (let [sink (-> (e/seq->stream s)
                   (e/filter pred)
                   e/stream->seq-ref)]
      @sink))
  (defn seq-map-seq [f s]
    (let [sink (-> (e/seq->stream s)
                   (e/map f)
                   e/stream->seq-ref)]
      @sink))
  (defn matching-seqs? [s1 s2]
    (every? (fn [[a b]] (= a b))
            (map vector s1 s2)))

  (defn twoseq-interleave [s1 s2]
    (let [src1 (delayed-seq->stream s1 50)
          src2 (delayed-seq->stream s2 100)
          sink (-> (e/interleave src1 src2)
                   e/stream->seq-ref)]
      @sink))

  (is (matching-seqs?
       (range 10)
       (seq-seq (range 10))))

  (is (matching-seqs?
       (filter even? (range 10))
       (seq-filter-seq even? (range 10))))

  (is (matching-seqs?
       (map (partial + 1) (range 10))
       (seq-map-seq (partial + 1) (range 10))))

  (is (matching-seqs?
       (concat (range 10) (range 20 30))
       (twoseq-interleave (range 10) (range 20 30))))
  )