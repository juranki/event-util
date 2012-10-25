(ns event-util.core
  (:refer-clojure :exclude [filter map interleave]))

(defprotocol EventSink
  (on-value [sink val])
  (on-done  [sink]))

(defprotocol EventSource
  (subscribe   [source sink])
  (unsubscribe [source sink]))

(defn simple-source [sinks-set-ref]
  (reify EventSource
    (subscribe   [_ sink] (dosync (alter sinks-set-ref conj sink)))
    (unsubscribe [_ sink] (dosync (alter sinks-set-ref disj sink)))))

(defn stream->seq-ref [stream]
  (let [buf (ref [])
        rv  (promise)]
    (subscribe stream (reify EventSink
                        (on-value [_ val] (dosync (alter buf conj val)))
                        (on-done  [_]     (deliver rv @buf))))
    rv))

(defn seq->stream [coll]
  (reify EventSource
    (subscribe [x sink]
      (future
        (Thread/sleep 500) ; TODO
        (doseq [elem coll] (on-value sink elem))
        (on-done sink)))
    (unsubscribe [x sink] true)))

(defn filter [source pred]
  (let [sinks (ref #{})]
    (subscribe source
               (reify EventSink
                 (on-value [_ val]
                   (if (apply pred [val])
                     (doseq [sink @sinks] (on-value sink val))))
                 (on-done [_] (doseq [sink @sinks] (on-done sink)))))
    (simple-source sinks)))

(defn map [source f]
  (let [sinks (ref #{})]
    (subscribe source
               (reify EventSink
                 (on-value [_ val]
                   (let [v (apply f [val])]
                     (doseq [sink @sinks] (on-value sink v))))
                 (on-done [_] (doseq [sink @sinks] (on-done sink)))))
    (simple-source sinks)))

(defn interleave [& sources]
  (let [srcs (ref (set sources))
        sinks (ref #{})]
    (doseq [src @srcs]
      (subscribe src (reify EventSink
                       (on-value [_ val]
                         (doseq [sink @sinks] (on-value sink val)))
                       (on-done  [_]
                         (dosync (alter srcs disj src))
                         (if (empty? @srcs)
                           (doseq [sink @sinks] (on-done sink)))))))
    (simple-source sinks)))