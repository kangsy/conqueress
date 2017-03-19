(ns conqueress.consumer
  (:require [mount.core :refer [defstate]]
            [clojure.core.async :refer [put! chan <! >! go-loop close!] :as a]
            [taoensso.timbre :as log]

            [franzy.serialization.nippy.deserializers :as nippy-deserializers]
            [kinsky.client      :as client]
            [kinsky.async       :as async]

            ;; todo refactor failure out of http-end-ns
            [conqueress.config :refer [config]]
            [conqueress.failure :refer [fail]]))



(defn create-kafka-channel
  ""
  [topic]
  (let [
        value-deserializer (nippy-deserializers/nippy-deserializer)
        [out ctl] (async/consumer config
                                  :keyword
                                  value-deserializer)
        pipe-trans (fn [ci xf]
                     (let [co (chan 1 xf)]
                       (a/pipe ci co)
                       co))
        filtered (pipe-trans out (filter #(= (:type %) :record)))
        ]

    (a/put! ctl {:op :subscribe :topic topic})
    [filtered ctl]))


(defstate events-ch
  :start (create-kafka-channel (:events.channel config))
  :stop (a/put! (second events-ch) {:op :stop}))

(defstate events-pub
  :start (a/pub (first events-ch) (comp :parent :value)))

(defstate query-ch
  :start (create-kafka-channel (:query.channel config))
  :stop (do (a/put! (second query-ch) {:op :stop})
            (a/close! (first query-ch))))

(defstate commands-ch
  :start (create-kafka-channel (:commands.channel config))
  :stop (do (a/put! (second commands-ch) {:op :stop})
            (a/close! (first commands-ch))))
