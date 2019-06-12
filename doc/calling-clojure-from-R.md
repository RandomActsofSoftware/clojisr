In this tutorial we show how to call Clojure from R using Renjin.

Renjin, when used [as an R
package](http://docs.renjin.org/en/latest/package/), allows to run R
code on that JVM and pass data to/from it, for most of the main R
datatypes.

Demo
----

### Setup

    options(java.parameters = c("-Xmx1g")) # Set your JVM options before running the JVM
    library(rJava)
    library(renjin) # Loading this library actually runs the JVM already.

    # Add the Clojuress JAR to the classpath.
    .jaddClassPath("../target/clojuress-0.1.0-SNAPSHOT-standalone.jar")

### Running code

The following function will allow us to run Clojure code.

We define it using a Renjin block, that runs on the JVM and covnerts the
returned value from a Renjin object to an R object. For more details
about this way of using Renjin, look
[here](http://docs.renjin.org/en/latest/package/index.html) at the
Renjin docs.

    clj <- function(code) renjin({ # This block runs on the JVM through Renjin.
      import(clojure.java.api.Clojure)
      ## Given a qualified name of a clojure var, we can get its value.
      clj_get <- (function(name) Clojure$var(name))
      ## Given an unqualified name, let us take it from the clojure.core namespace:
      clj_core_get <- (function(name) Clojure$var("clojure.core", name))
      ## Let us get some Clojure functions (as Java objects implementing the interface IFn):
      clj_eval <- clj_core_get("eval")
      require <- clj_core_get("require")
      read_string <- clj_core_get("read-string")
      comp <- clj_core_get("comp")
      ## IFn has a method named 'invoke', that can be used in renjin as a function.
      ##
      ## For example, let us invoke the clojure function 'comp':
      eval_read_string <-
          comp$invoke(
                   clj_eval,
                   read_string)
      ## Using 'comp', we just composed two Clojure functions and got a new one, that:
      ## It reads a given string of clojure code,
      ## evaluates the corresponding form
      ## and returns the result.
      ##
      ## Let us use this function to require a namespace from Clojuress.
      eval_read_string$invoke("(require 'clojuress.renjin.to-renjin)")
      ## Now we may use functions of these namespace:
      to_renjin <- clj_get("clojuress.renjin.to-renjin/->renjin")
      ## Let us create yet another function composition:
      eval_read_string_to_renjin <-
          comp$invoke(
                   to_renjin,
                   clj_eval,
                   read_string)
      ## We just created a Clojure function that does the following:
      ## Reads a given string of clojure code,
      ## evaluates the corresponding form,
      ## converts the result to a renjin object
      ## and returns that object.
      ##
      ## Let us use this function to read, evaluate and convert the code we got:
      eval_read_string_to_renjin$invoke(code)
      ## The returned value of this block is the result of this evaluated.
      ## It will be converted to an R object, if Renjin knows how.
      ## For many datatypes, Renjin does know how.
    })

Let us try it:

    clj("(+ 1 2)")

    ## [1] 3

    clj("{:x [1 nil 2] :y [:A :B]}")

    ## $x
    ## [1]  1 NA  2
    ## 
    ## $y
    ## [1] A B
    ## Levels: A B

### Using functions

The following function will allow us to use a clojure function as an R
function. The R arguments are be converted to a Clojure data structure,
and the return value is covnerted back from Clojure to R.

    clj_fn <- function(fn_code) function(args) {
        renjin({
            import(clojure.java.api.Clojure)
            clj_get <- (function(name) Clojure$var(name))
            clj_core_get <- (function(name) Clojure$var("clojure.core", name))
            clj_eval <- clj_core_get("eval")
            require <- clj_core_get("require")
            read_string <- clj_core_get("read-string")
            comp <- clj_core_get("comp")
            eval_read_string <-
                comp$invoke(
                         clj_eval,
                         read_string)
            eval_read_string$invoke("(require 'clojuress.renjin.to-renjin)")
            to_renjin <- clj_get("clojuress.renjin.to-renjin/->renjin")
            eval_read_string$invoke("(require 'clojuress.renjin.to-clj)")
            to_clj <- clj_get("clojuress.renjin.to-clj/->clj")
            eval_read_string_to_renjin <-
                comp$invoke(
                         to_renjin,
                         clj_eval,
                         read_string)
            import(clojure.java.api.Clojure)
            clj_core_get <- (function(name) Clojure$var("clojure.core", name))
            fn_from_code <- eval_read_string_to_renjin$invoke(fn_code)
            f <- comp$invoke(
                          to_renjin,
                          fn_from_code,
                          to_clj)
            f$invoke(args)
        })
    }

Let us use it:

    clj_fn("(partial map (partial * 10))")(1:9)

    ## [1] 10 20 30 40 50 60 70 80 90

    clj_fn("(partial group-by even?)")(1:9)

    ## $`FALSE`
    ## [1] 1 3 5 7 9
    ## 
    ## $`TRUE`
    ## [1] 2 4 6 8

    clj("(require '[com.rpl.specter :refer [transform ALL MAP-VALS]]) nil")

    ## NULL

    clj_fn("#(identity (transform [ALL MAP-VALS ALL] (partial * 100) %))")(
        list(list(x=1:3,
                  y=(1:3)^2),
             list(x=1:4,
                  y=(1:4)^2)))

    ## [[1]]
    ## [[1]]$x
    ## [1] 100 200 300
    ## 
    ## [[1]]$y
    ## [1] 100 400 900
    ## 
    ## 
    ## [[2]]
    ## [[2]]$x
    ## [1] 100 200 300 400
    ## 
    ## [[2]]$y
    ## [1]  100  400  900 1600

Discussion
----------