(defproject com.kangrd/conqueress "0.1.0"
  :description "Conqueress is a basic implementation of CQRS. This library helps kickstarting a project with Kafka by providing some standard usage patterns."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/core.async "0.2.395"]
                 
                 [com.taoensso/timbre "4.7.4"]

                 [org.clojure/algo.monads "0.1.6"]
                 [danlentz/clj-uuid "0.1.6"]

                 [mount "0.1.10"]

                 [ymilky/franzy-nippy "0.0.1"]
                 [spootnik/kinsky "0.1.15"]
                 ]
  )
