(ns conqueress.monads
  (:use 
   [clojure.algo.monads :only [defmonad domonad]]
   [conqueress.failure]
   )
  )

(defmonad error-m 
  [m-result identity
   m-bind   (fn [m f]  (if (and m (has-failed? m))
                         m
                         (f m)))])

(defmacro attempt-all 
  ([bindings return] `(domonad error-m ~bindings ~return))
  ([bindings return else]
   `(let [result# (attempt-all ~bindings ~return)]
      (if (has-failed? result#) 
        (if (fn? ~else) (~else result#) ~else)
        result#))))
