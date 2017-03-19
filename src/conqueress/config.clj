(ns conqueress.config
  (:require [mount.core :refer [defstate]])
  )

(defstate config
  :start {:bootstrap.servers "localhost:9092"
          :group.id "conqueress-consumer"
          :events.channel "events"
          :query.channel "query"
          :commands.channel "commands"
          })
