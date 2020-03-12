(ns clojisr.v1.tutorial-test
  (:require [notespace.v2.note :as note
             :refer [note note-void note-md note-as-md note-hiccup note-as-hiccup check]]
            [notespace.v2.live-reload]))

(note-md "# Clojisr tutorial")

(note-md :basic-examples "## Basic examples")

(note-md "Let us start by some basic usage examples of Clojisr.")

(note-void
 (require '[clojisr.v1.r :as r :refer [r eval-r->java r->java java->r java->clj java->naive-clj clj->java r->clj clj->r ->code r+ colon]]
          '[clojisr.v1.require :refer [require-r]]
          '[clojisr.v1.robject :as robject]
          '[clojisr.v1.session :as session]
          '[clojisr.v1.rserve :as rserve]
          '[tech.ml.dataset :as dataset]))

(note-md "First, let us make sure that we use the Rserve backend (in case we were using Renjin instead earlier), and that there are no R sessions currently running.")

(note-void
 (rserve/set-as-default!)
 (r/discard-all-sessions))

(note-md "Now let us run some R code, and keep a Clojure handle to the return value.")

(note-void
 (def x (r "1+2")))

(note-md "Convert the R to Clojure:
This part requires more thorough documentation.")

(note
 (->> x
      r->clj
      (check = [3.0])))

(note
 (->> "list(A=1,B=2,'#123strange<text> ()'=3)"
      r
      r->clj
      (check = {:A [1.0], :B [2.0], "#123strange<text> ()" [3.0]})))

(note-md "Run some code on a separate session (specified Rserve port, rather than the default one).")

(note
 (-> "1+2"
     (r :session-args {:port 4444})
     r->clj
     (->> (check = [3.0]))))

(note-md "Convert Clojure data to R data. Note that `nil` is turned to `NA`.")

(note
 (-> [1 nil 3]
     clj->r))

(note-md  :functions "## Functions")

(note-md "An R function is also a Clojure function.")

(note-void
 (def f (r "function(x) x*10")))

(note-md "Let us apply it to Clojure data (implicitly converting that data to R).")

(note
 (->> 5
      f
      r->clj
      (check = [50.0])))

(note-md "We can also apply it to R data.")

(note
 (->> "5*5"
      r
      f
      r->clj
      (check = [250.0])))

(note-md "Functions can get named arguments.
Here we pass the `na.rm` argument,
that tells R whether to remove missing values
whenn computing the mean.")

(note
 (->> ((r "mean")
       [1 nil 3]
       :na.rm true)
      r->clj
      (check = [2.0])))

(note-md "Another example:")

(note
 (let [f (r "function(w,x,y=10,z=20) w+x+y+z")]
   (->> [(f 1 2)
         (f 1 2 :y 100)
         (f 1 2 :z 100)]
        (map r->clj)
        (check = [[33.0] [123.0] [113.0]]))))

(note-md "Some functions are already created in Clojisr and given special names for convenience. For example:")

(note
 (->> (r+ 1 2 3)
      r->clj
      (check = [6.0])))

(note
 (->> (colon 0 9)
      r->clj
      (check = (range 10))))

(note-md  :R-dataframes-and-tech.ml.dataset
          "## R dataframes and [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) datasets")

(note-md "Create a tech.ml.dataset dataset object,
pass it to an R function to compute the row means,
and convert the return value to Clojure.")

(note
 (let [row-means (r "function(data) rowMeans(data)")]
   (->> {:x [1 2 3]
         :y [4 5 6]}
        dataset/name-values-seq->dataset
        row-means
        r->clj
        (check = [2.5 3.5 4.5]))))

(note-md "Load the R package 'dplyr' (assuming it is installed).")

(note-void
 (r "library(dplyr)"))

(note-md "Use dplyr to process some Clojure dataset, and convert back to the resulting dataset.")

(note
 (let [filter-by-x  (r "function(data) filter(data, x>=2)")
       add-z-column (r "function(data) mutate(data, z=x+y)")]
   (->> {:x [1 2 3]
         :y [4 5 6]}
        dataset/name-values-seq->dataset
        filter-by-x
        add-z-column
        r->clj
        (check (fn [d]
                 (-> d
                     dataset/->flyweight
                     (= [{:x 2.0 :y 5.0 :z 7.0}
                         {:x 3.0 :y 6.0 :z 9.0}])))))))

(note-md "Tibbles are also supported, as a special case of data frames.")

(note-void
 (r "library(tibble)"))

(note
 (let [tibble (r "tibble")]
   (tibble
    :x [1 2 3]
    :y [4 5 6])))

(note
 (let [tibble (r "tibble")]
   (->> (tibble
         :x [1 2 3]
         :y [4 5 6])
        r->clj
        dataset/->flyweight
        (check = [{:x 1.0 :y 4.0}
                  {:x 2.0 :y 5.0}
                  {:x 3.0 :y 6.0}]))))

(note-md :R-objects
         "## R objects")

(note-md "Clojisr holds handles to R objects, that are stored in memory at the R session, where they are assigned random names.")

(note-void
 (def one+two (r "1+2")))

(note
 (->> one+two
      class
      (check = clojisr.v1.robject.RObject)))

(note-md "We can figure out the place in R memory corresponding to an object's name.")

(note (:object-name one+two))

(note-md :generating-code
         "## Generating code")

(note-md "Let us see the code-generation mechanism of Clojisr, and the rules defining it.")

(note-md "We will need a reference to the R session:")

(note-void
 (def session
   (session/fetch-or-make nil)))

(note-md "For the following examples, we will use some dummy handles to R objects with given names:")

(note-void
 (def x (robject/->RObject "x" session nil nil))
 (def y (robject/->RObject "robject_y" session nil nil)))

(note-md ".. and some real handles to R objects:")

(note-void
 (def minus-eleven (r "-11"))
 (def abs (r "abs")))

(note-md "For an r-object, we generate the code which is identical to r-object name")

(note
 (->> x
      ->code
      (check = "x")))

(note-md "For a clojure value, we use sofisticated form analysis and generate proper R string or values.")

(note
 (->> "hello"
      ->code
      (check re-matches #"\"hello\"$")))

(note
 (->> [1 2 3]
      ->code
      (check = "c(1,2,3)")))


(note-md "For a symbol, we generate the code with the corresponding R symbol.")

(note (->code 'x))

(note-md "A sequential structure (list, vector, etc.) can be interpreted as a compound expression, for which code generation is defined accorting to the first list element.")

(note-md "For a list beginning with the symbol `'function`, we generate an R function definition.")

(note (->> '(function [x y] x)
           ->code
           (check = "function(x,y) {x}")))

(note-md "For a vector instead of list, we create R vector.")

(note (->> '[function [x y] x]
           ->code
           (check = "c(function,c(x,y),x)")))

(note-md "For a list beginning with the symbol `'formula`, we generate an R `~`-formula.")

(note (->> '(formula x y)
           ->code
           (check = "(x~y)")))

(note-md "For a list beginning with a symbol known to be a binary operator, we generate nested calls.")

(note (->> '(+ x y z)
           ->code
           (check = "((x+y)+z)")))

(note-md "For a list beginning with another symbol, we generate a function call with that symbol as the function name.")

(note (->> '(f x)
           ->code
           (check = "f(x)")))

(note-md "For a list beginning with an R object that is a function, we generate a function call with that object as the function. Don't forget to unquote name if it's defined in Clojure.")

(note (->> '(~abs x)
           ->code
           (check re-matches #"\.MEM\$.*\(x\)")))

(note-md "All other sequential things (that is, those not beginning with a symbol or R function) are intepreted as data, converted implicitly R data representation.")

(note (->> `(~abs (1 2 3))
           ->code
           (check re-matches #"\.MEM\$.*\(c\(1,2,3\)\)")))

(note-md "Some more examples, showing how these rules compose:")

(note (->code '(function [x y] (f y))))
(note (->code '(function [x y] (f ~y))))

(note (->code '(function [x y] (+ x y))))
(note (->code (list 'function '[x y] (list '+ 'x 'y))))

(note (->code '(function [x y] (print x) (f x))))

(note (->code '(function [x y] (~abs x))))

(note (->code '(~abs ~minus-eleven)))

(note (->code '(~abs -11)))

(note-md "Use syntax quote in case you want to use local bindings")

(note (let [minus-ten -10]
        (->code `(~abs ~minus-ten))))

(note-md :running-generated-code
         "## Running generated code")

(note-md "Clojure forms can be run as R code. For example:")

(note (->> '(~abs ~(range -3 0))
           r
           r->clj
           (check = [3.0 2.0 1.0])))

(note-md "Let us repeat the basic examples from the beginning of this tutorial,
this time generating code rather than writing it as Strings.")

(note-void
 (def x (r '(+ 1 2))))

(note
 "checking again... "
 (->> x
      r->clj
      (check = [3.0])))

(note-void
 (def f (r '(function [x] (* x 10)))))

(note
 "checking again... "
 (->> 5
      f
      r->clj
      (check = [50.0])))

(note
 "checking again... "
 (->> "5*5"
      r
      f
      r->clj
      (check = [250.0])))

(note
 (let [row-means (r '(function [data] (rowMeans data)))]
   (->> {:x [1 2 3]
         :y [4 5 6]}
        dataset/name-values-seq->dataset
        row-means
        r->clj
        (check = [2.5 3.5 4.5]))))

(note-void
 (r '(library dplyr)))

(note
 (let [filter-by-x  (r '(function [data] (filter data (>= x 2))))
       add-z-column (r '(function [data] (mutate data (= z (+ x y)))))]
   (->> {:x [1 2 3]
         :y [4 5 6]}
        dataset/name-values-seq->dataset
        filter-by-x
        add-z-column
        r->clj)))

(note-md "The strange column name is due to dplyr's mutate behaviour when extra parens are added to the expression.")

(note-md :requiring-R-packages
         "## Requiring R packages")

(note-md "Sometimes, we want to bring to the Clojure world functions and data from R packages.
Here, we try to follow the [require-python](https://github.com/cnuernber/libpython-clj/blob/master/test/libpython_clj/require_python_test.clj) syntax
of [libpython-clj](https://github.com/cnuernber/libpython-clj)
(though currently in a less sophisticated way.)")

(note-void
 (require-r '[stats :as statz :refer [median]]))

(note
 (->> [1 2 3]
      r.stats/median
      r->clj
      (check = [2.0])))

(note
 (->> [1 2 3]
      statz/median
      r->clj
      (check = [2.0])))

(note
 (->> [1 2 3]
      median
      r->clj
      (check = [2.0])))

(note-void
 (require-r '[datasets :as datasetz :refer [euro]]))

(note (->> [r.datasets/euro
            datasetz/euro
            euro]
           (check apply =)))

(note-void
 (require-r '[base :refer [$]]))

(note
 (-> {:a 1 :b 2}
     ($ 'a)
     r->clj
     (->> (check = [1]))))

(note-md :data-visualization
         "## Data visualization")

(note-md "Functions creating R plots or any plotting objects generated by various R libraries can be wrapped in a way that returns an SVG, BufferedImage or can be saved to a file. All of them accept additional parameters specified in `grDevices` R package.")

(note-md "Currently there is a bug that sometimes causes axes and labels to disappear when rendered inside a larger HTML.")

(note-void
 (require-r '[graphics :refer [plot hist]])
 (require-r '[ggplot2 :refer [ggplot aes geom_point xlab ylab labs]])
 (require '[clojisr.v1.applications.plotting :refer [plot->svg plot->file plot->buffered-image]]))

(note-md "First example, simple plotting function as SVG string.")

(note-as-hiccup
 (plot->svg
  (fn []
    (->> rand
         (repeatedly 30)
         (reductions +)
         (plot :xlab "t"
               :ylab "y"
               :type "l")))))

(note-md "ggplot2 plots (or any other plot objects like lattice) can be also turned into SVG.")

(note-as-hiccup
 (plot->svg
  (let [x (repeatedly 99 rand)
        y (map +
               x
               (repeatedly 99 rand))]
    (-> {:x x :y y}
        dataset/name-values-seq->dataset
        (ggplot (aes :x x
                     :y y
                     :color '(+ x y)
                     :size '(/ x y)))
        (r+ (geom_point)
            (xlab "x")
            (ylab "y"))))))

(note-md "Any plot (function or object) can be saved to file or converted to BufferedImage object.")

(def target-path (notespace.v2.note/ns->out-dir *ns*))

(note (r->clj (plot->file (str target-path "histogram.jpg") (fn [] (hist [1 1 1 1 2 3 4 5]))
                          :width 800 :height 400 :quality 50)))

(note-hiccup [:image {:src "histogram.jpg"}])

(note (plot->buffered-image (fn [] (hist [1 1 1 1 2 3 4 5])) :width 222 :height 149))

(note-md :intermediary-representation-as-Java-objects
         "## Intermediary representation as Java objects.")

(note-md "Clojisr relies on the fact of an intemediary representation of java, as Java objects. This is usually hidden from the user, but may be useful sometimes.
In the current implementation, this is based on [REngine](https://github.com/s-u/REngine).")

(note-void
 (import (org.rosuda.REngine REXP REXPInteger REXPDouble)))

(note-md "We can convert data between R and Java.")

(note
 (->> "1:9"
      r
      r->java
      class
      (check = REXPInteger)))

(note
 (->> (REXPInteger. 1)
      java->r
      r->clj
      (check = [1])))

(note-md "We can further convert data from the java representation to Clojure.")

(note
 (->> "1:9"
      r
      r->java
      java->clj
      (check = (range 1 10))))

(note-md "On the opposite direction, we can also convert Clojure data into the Java represenattion.")

(note
 (->> (range 1 10)
      clj->java
      class
      (check = REXPInteger)))

(note
 (->> (range 1 10)
      clj->java
      java->clj
      (check = (range 1 10))))

(note-md "There is an alternative way of conversion from Java to Clojure, naively converting the internal Java representation to a Clojure data structure. It can be handy when one wants to have plain access to all the metadata (R attributes), etc. ")

(note
 (->> "1:9"
      r
      r->java
      java->naive-clj))

(note
 (->> "data.frame(x=1:3,y=factor('a','a','b'))"
      r
      r->java
      java->naive-clj))

(note-md "We can evaluate R code and immediately return the result as a java object, without ever creating a handle to an R object holding the result:")

(note
 (->> "1+2"
      eval-r->java
      class
      (check = REXPDouble)))

(note
 (->> "1+2"
      eval-r->java
      (.asDoubles)
      vec
      (check = [3.0])))

(note-md :more-data-conversion-examples
         "## More data conversion examples")

(note-md "Convertion between R and Clojure always passes through Java.
To stress this, we write it explicitly in the following examples.")

(note
 (->> "list(a=1:2,b='hi!')"
      r
      r->java
      java->clj
      (check = {:a [1 2] :b ["hi!"]})))

(note
 (->> "table(c('a','b','a','b','a','b','a','b'), c(1,1,2,2,3,3,1,1))"
     r
     r->java
     java->clj
     (check = {["1" "a"] 2 ["1" "b"] 2 ["2" "a"] 1 ["2" "b"] 1 ["3" "a"] 1 ["3" "b"] 1})))


(note
 (->> {:a [1 2] :b "hi!"}
      clj->java
      java->r
      r->java
      java->clj
      (check = {:a [1 2] :b ["hi!"]})))

(note
 (->> {:a [1 2] :b "hi!"}
      clj->java
      java->r
      ((r "deparse"))
      r->java
      java->clj))

(note-md "### Basic types convertion clj->r->clj")

(note (def clj->r->clj (comp r->clj r)))

(note (check = (clj->r->clj nil) nil))
(note (check = (clj->r->clj [10 11]) [10.0 11.0]))
(note (check = (clj->r->clj [10.0 11.0]) [10.0 11.0]))
(note (check = (clj->r->clj (list 10.0 11.0)) [10.0 11.0]))
(note (check = (clj->r->clj {:a 1 :b 2}) {:a [1] :b [2]}))

(note-md "### Various R objects")

(note-md "Named list")
(note (->> (r "list(a=1,b=c(10,20),c='hi!')") ;; named list
           r->clj
           (check = {:a [1.0] :b [10.0 20.0] :c ["hi!"]})))

(note-md "Array of doubles")
(note (->> (r "c(10,20,30)") ;; array of doubles
           r->clj
           (check = [10.0 20.0 30.0])))

(note-md "Timeseries")
(note (->> (r r.datasets/euro) ;; timeseries
           r->clj
           first
           (check = 13.7603)))

(note-md "Pairlist")
(note (->> (r.base/formals r.stats/dnorm) ;; pairlist
           r->clj
           keys
           sort
           (check = '(:log :mean :sd :x))))

(note-md "NULL")
(note (->> (r "NULL") ;; null
           r->clj
           (check = nil)))

(note-md "TRUE/FALSE")
(note (->> (r "TRUE") ;; true/false
           r->clj
           (check = [true]))) ;; why?

(note-md :inspecting-R-functions
         "## Inspecting R functions")

(note-md "The `mean` function is defined to expect arguments `x` and `...`.
These arguments have no default values (thus, its formals have empty symbols as values):")

(note
 (->> 'mean
      r.base/formals
      r->clj
      (check = {:x (symbol "")
                :... (symbol "")})))

(note-md "It is an [S3 generic function](http://adv-r.had.co.nz/S3.html) function, which we can realize by printing it:")

(note
 (r 'mean))

(note-md "So, we can expect possibly more details when inspecting its default implementation.
Now, we see some arguments that do have default values.")

(note
 (->> 'mean.default
      r.base/formals
      r->clj
      (check = {:x     (symbol "")
                :trim  [0.0]
                :na.rm [false]
                :...   (symbol "")})))

(note-md :R-function-arglists
         "## R-function-arglists")

(note-md "As we saw earlier, R functions are Clojure functions. The arglists of functions brought up by `require-r` match the expected arguments. Here are some examples:")

(note-void (require-r
            '[base]
            '[stats]
            '[grDevices]))

(note
 (->> [#'r.base/mean, #'r.base/mean-default, #'r.stats/arima0,
       #'r.grDevices/dev-off, #'r.base/Sys-info, #'r.base/summary-default
       ;; Primitive functions:
       #'r.base/sin, #'r.base/sum]
      (map (fn [f]
             (-> f
                 meta
                 (update :ns (comp symbol str)))))
      (check
       =
       '({:arglists ([x & {:keys [...]}]), :name mean, :ns r.base}
         {:arglists ([x & {:keys [trim na.rm ...]}]),
          :name     mean-default,
          :ns       r.base}
         {:arglists
          ([x & {:keys
                 [order seasonal xreg include.mean delta
                  transform.pars fixed init method n.cond
                  optim.control]}]),
          :name arima0,
          :ns   r.stats}
         {:arglists ([& {:keys [which]}]),
          :name dev-off,
          :ns r.grDevices}
         {:arglists ([]),
          :name Sys-info,
          :ns r.base}
         {:arglists ([object & {:keys [... digits quantile.type]}]),
          :name summary-default,
          :ns r.base}
         {:arglists ([x]), :name sin, :ns r.base}
         {:arglists ([& {:keys [... na.rm]}]), :name sum, :ns r.base}))))

(note-md :using-Renjin
         "## Using Renjin")

(note-md "In the followint example, we use a differnt R backend (the pure JVM Renjin) for reading a csv, without changing the default backend (which is the usual R using Rserve).")

(note
 (require 'clojisr.v1.renjin)
 (let [path "/tmp/data.csv"]
   (spit path "a,b,c\n1,2,3\n4,5,6\n")
   (-> `(read.csv ~path)
       (r :session-args {:session-type :renjin})
       (r/r->clj :session-args {:session-type :renjin})
       (->> (check = [{:a 1, :b 2, :c 3}
                      {:a 4, :b 5, :c 6}])))))

(notespace.v2.note/compute-this-notespace!)
