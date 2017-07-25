# goo

> With no ability to interact or observe the other side of these keyholes, nanoengineers at the University of Manchester created a swarm intelligent system called the Pathfinder (Goo), its prime directive: <b>Explore, Gather, Report</b>.<br> - [aliens.wikia.com](http://aliens.wikia.com/wiki/Goo)

A wrapper to the [Iapetos](https://github.com/xsc/iapetos) clojure binding for the prometheus client.
XSC did some great work there, however handling the registry in different places in your program appeared a bit cumbersome to us.
Goo yields a default-registry in it's state which is used by default for most operations (inc, set...) on collectors. 