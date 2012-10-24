(ns event-util.core)

(defprotocol EventSink
  (on-value [sink val])
  (on-done  [sink])
  (on-error [sink err]))

(defprotocol EventSource
  (subscribe   [source sink])
  (unsubscribe [source sink]))

(def sink-defaults
  {:on-value (fn [_val] true)
   :on-done  (fn [] true)
   :on-error (fn [err] (throw err))})

(def dispathcer-defaults
  {:on-value    (fn [sinks val]
                  (doseq [sink @sinks] (on-value sink val)))
   :on-done     (fn [sinks]
                  (doseq [sink @sinks] (on-done  sink)))
   :on-error    (fn [sinks err]
                  (doseq [sink @sinks] (on-error sink err)))
   :subscribe   (fn [sinks sink]
                  (dosync (alter sinks conj sink)))
   :unsubscribe (fn [sinks sink]
                  (dosync (alter sinks disj sink)))})

(defn make-dispatcher [fn-overrides]
  (let [fns   (merge dispathcer-defaults fn-overrides)
        sinks (ref #{})]
    (reify

      EventSink
      (on-value [_ value] (apply (:on-value fns) [sinks value]))
      (on-done  [_]       (apply (:on-done  fns) [sinks]))
      (on-error [_ err]   (apply (:on-error fns) [sinks err]))

      EventSource
      (subscribe   [_ sink] (apply (:subscribe   fns) [sinks sink]))
      (unsubscribe [_ sink] (apply (:unsubscribe fns) [sinks sink])))))

(defn make-sink [fn-overrides]
  (let [fns (merge sink-defaults fn-overrides)]
    (reify EventSink
      (on-value [sink value] (apply (:on-value fns) [value]))
      (on-done  [sink]       (apply (:on-done fns)  []))
      (on-error [sink err]   (apply (:on-error fns) [err])))))

(defn stream->seq-ref [stream]
  (let [buf (ref [])
        rv  (promise)
        sink (make-sink
              {:on-value #(dosync (alter buf conj %))
               :on-done  #(deliver rv @buf)})]
    (subscribe stream sink)
    rv))

(defn seq->stream [coll]
  (reify EventSource
    (subscribe [x sink]
      (future
        (Thread/sleep 500) ; TODO
        (doseq [elem coll] (on-value sink elem))
        (on-done sink)))
    (unsubscribe [x sink] true)))

(defn filter [pred stream]
  (let [default   (:on-value dispathcer-defaults)
        override  {:on-value (fn [sinks val]
                               (if (apply pred [val])
                                 (apply default [sinks val])))}
        disp      (make-dispatcher override)]
    (subscribe stream disp)
    disp))
