(ns conqueress.core
  (:require
   [taoensso.timbre :as log]
   [clj-uuid :as uuid]

   [clojure.spec :as s]

   [conqueress.failure :refer [fail has-failed? failure?]]
   [conqueress.producer :as p]
   [conqueress.monads :refer [attempt-all]]
   [conqueress.commands :as commands]
   [conqueress.consumer :refer [events-pub]]


   [clojure.core.async :as a])
  (:import (org.apache.kafka.clients.producer ProducerRecord)))

(defn- spec-name [spec]
  (cond
    (ident? spec) spec
    (s/regex? spec) (::name spec)
    (instance? clojure.lang.IObj spec)
    (-> (meta spec) ::name)))

(defn- ->sym
  "Returns a symbol from a symbol or var"
  [x]
  (if (var? x)
    (let [^clojure.lang.Var v x]
      (symbol (str (.name (.ns v)))
              (str (.sym v))))
    x))

(defn validate-args
  ""
  [cmdfn args]
  (let [
        cmdsp (s/get-spec cmdfn)
        argspec (when cmdsp (:args cmdsp))]

    (log/debug ::args args)
    (if argspec
      (if (s/invalid? (s/conform argspec args))
        (let [ed (assoc (s/explain-data*
                         argspec [:args]
                         (if-let [name (spec-name argspec)]
                           [name] []) [] args)
                        ::args args)]
          (log/error "args not conform ")
          (fail (str
                 "Call to " (->sym cmdfn) " did not conform to spec:\n"
                 (with-out-str (s/explain-out ed)))))
        args)
      true)))

(defn command-handler
  [ctx]
  (let [
        cmd (:cmd ctx)
        args (:args ctx)
        ]
    (attempt-all
     [command (get @commands/registry cmd)
      valid-args (when command (validate-args (:fn command) args))
      eid (uuid/v1)
      data (assoc ctx :eid eid) 
      _ (p/send! p/producer "commands" data)
      _ (log/debug ::comand-handler :command command)
      result (or (when (= :sync (:result-handling command))
                   (let [
                         rch (a/promise-chan)
                         _ (a/sub events-pub eid rch)
                         ]
                     (try
                       (a/alt!!
                         rch
                         ([v] (:value v))
                         (a/timeout 1500)
                         ([v] (fail {:form :time-out})))
                       (finally
                         (a/close! rch)
                         (a/unsub events-pub eid rch)))
                     ))
                 data)
      _ (log/debug ::failed1?  (failure? (:result result)) (has-failed? (:result result)))
      _ (log/debug ::failed? (:result result) (type (:result result)) (has-failed? (:result result)))
      _ (log/debug ::command-handler :result  result )
      catch-failure (when (has-failed? (:result result)) (fail (.errors (:result result))))
      ]
     result
     )
    ))

(defn query-handler
  [ctx]
  (let [
        db (:db ctx)
        col (:col ctx)
        query (:query ctx)]

    (log/debug ::query-handler ctx)
    (attempt-all
     [
      ;; TODO validate, acl
      eid (uuid/v1)
      data {:eid eid
            :query query
            :col col
            :db db}
      _ (log/debug ::query-handler :sending-query data)
      _ (p/send! p/producer "query" data)
      result (let [
                   rch (a/promise-chan)
                   _ (a/sub events-pub eid rch)
                   ]
               (try
                 (a/alt!!
                   rch
                   ([v] v)
                   (a/timeout 2000)
                   ([v] (fail :time-out)))
                 (finally
                   (a/close! rch)
                   (a/unsub events-pub eid rch)))
               )
      _ (log/debug ::query-handler :result result (has-failed? result))
      catch-failure (when (has-failed? result) (fail (.errors result)))
      ]
     result
    )))
