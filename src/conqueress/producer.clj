(ns conqueress.producer
  (:require [mount.core :refer [defstate]]
            [kinsky.client      :as client]
            [kinsky.async       :as async]
            [franzy.serialization.nippy.serializers :as nippy-serializers]
            [taoensso.timbre :as log]

            [conqueress.config :refer [config]])
  (:import 
   (franzy.serialization.nippy.serializers NippySerializer)
   ))

(def value-serializer (nippy-serializers/nippy-serializer))
(defstate producer
  ;; config auslagern
  :start (client/producer config
                          :keyword
                          value-serializer)
  :stop (client/close! producer))

(defn send!
  ([producer topic data]
   (log/debug ::send! producer topic data )
   (client/send! producer topic nil data))
  ([producer topic key data]
   (log/debug ::send! producer topic key data )
   (client/send! producer topic key data))
  )
