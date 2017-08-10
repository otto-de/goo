# goo

> With no ability to interact or observe the other side of these keyholes, nanoengineers at 
 the University of Manchester created a swarm intelligent system called the Pathfinder (Goo), 
 its prime directive: <b>Explore, Gather, Report</b>.
 <br> - [aliens.wikia.com](http://aliens.wikia.com/wiki/Goo)

A wrapper to the [Iapetos](https://github.com/xsc/iapetos) clojure binding for the prometheus client.
XSC did some great work there, however handling the registry in different places in your program 
appeared a bit cumbersome to us. Goo yields a default-registry in it's state which is used by default 
for most operations (inc, set...) on collectors.
 
## Usage
 
It's easy. Register a metric ...

    (goo/register-counter! :search/products-found {:labels [:category]}) ;; register a counter with a "category" label
    
... and trigger a change

    (goo/inc! :search/products-found {:category "fashion"} 42)           ;; increment that counter by 42 while the category is "fashion"
    
    
You can register:

* Counters `(goo/register-counter! :namespaced/name option)`
  * Options can be `:description` and `:labels`
* Summaries `(goo/register-summary! :namespaced/name options)`
  * Options can be `:description`, `:labels` and `:quantiles`
* Histograms `(goo/register-histogram! :namespaced/name options)`
  * Options can be `:description`, `:labels` and `:buckets`
* Gauges `(goo/register-gauge! :namespaced/name options initial)`
  * Options can be `:description` and `:labels`
  * `initial` is a value that the gauge will be set on after initialization
* Callback-Gauges `(goo/register-callback-gauge! :namespaced/name description callback-fn label-values)`
  * the `callback-fn` is a sideeffect-free function that can determine the value of the gauge at every given moment
  * `label-values` holds a map of label _with_ their actual values
* Custom Metrics `(goo/register! some-iapetos-metric)`

You can trigger:

* increment on Counters `(goo/inc! :namespaced/name options)`
* decrement on Counters `(goo/dec! :namespaced/name options)`
* observe on Summaries and Histograms `(goo/observe! :namespaced/name options)`
* update on Gauges `(goo/update! :namespaced/name options)`
