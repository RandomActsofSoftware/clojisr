(ns clojisr.v1.impl.rserve.java
  (:require [clojure.pprint :as pp]
            [clojure.string :as string]
            [clojisr.v1.util :refer [exception-cause]]
            [clojure.tools.logging.readable :as log])
  (:import (org.rosuda.REngine REXP REXPString REXPSymbol REXPDouble REXPInteger REXPLanguage
                               RList REXPNull REngineException REXPMismatchException)
           (org.rosuda.REngine.Rserve RConnection)
           (java.util List Collection)
           (clojure.lang Named)))


(defn r-list [^Collection names
              ^Collection contents]
  (RList. contents names))

(defn rexp-language [^List alist]
  (REXPLanguage. alist))

(defn rexp-symbol [name]
  (REXPSymbol. name))

(defn rexp-double [xs]
  (REXPDouble. (double-array xs)))

(defn rexp-int [xs]
  (REXPInteger. (int-array xs)))

(defn call
  [op args]
  (REXP/asCall op (into-array REXP args)))

(defn assignment
  [varname java-obj]
  (call "<-"
        [(if (re-find #"\$" varname)
           (->> (string/split varname #"\$")
                (map rexp-symbol)
                (call "$"))
           (rexp-symbol varname))
         java-obj]))

(defn try-eval-catching-errors [expression ^RConnection r-connection]
  ;; Using the technique of https://stackoverflow.com/a/40447542/1723677, the way it is used in Rojure.
  (try
    (let [expression-str (-> expression
                             (string/escape char-escape-string)
                             (->> (format "try(eval(parse(text=\"%s\")),silent=TRUE)")))
          rexp (locking r-connection
                 (.parseAndEval r-connection expression-str))]
      (if (.inherits rexp "try-error")
        (do (log/error [::try-eval-catching-errors {:message (format "Error in R evaluating expression: %s. R exception: %s"
                                                                     expression (.asString rexp))}])
            (throw (Exception. (format "Error in R evaluating expression:\n %s.\nR exception: %s"
                                       expression (.asString rexp)))))
        rexp))
    (catch REngineException ex
      (log/error [::try-eval-catching-errors {:message (format "Caught exception evaluating expression: %s" expression)
                                              :exception (exception-cause ex)}]))
    (catch REXPMismatchException ex
      (log/error [::try-eval-catching-errors {:message (format "Caught exception evaluating expression: %s" expression)
                                              :exception (exception-cause ex)}]))))
