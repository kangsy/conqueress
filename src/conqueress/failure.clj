(ns conqueress.failure)

(defrecord Failure [errors])

(defprotocol ComputationFailed
  "A protocol that determines if a computation has resulted in a failure.
   This allows the definition of what constitutes a failure to be extended
   to new types by the consumer."
  (has-failed? [self]))

(extend-protocol ComputationFailed
  Object
  (has-failed? [self] false)

  nil
  (has-failed? [self] false)

  Failure
  (has-failed? [self] true)

  Exception 
  (has-failed? [self] true))

(defn fail [errors]
  (Failure. errors))

(def failure? (fn [x]
                (when (instance? Failure x) x)))
