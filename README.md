# ClojureScript One

ClojureScript One gives you a working client/server application 
with ClojureScript and Clojure code reloading, a way to do templating, easy
access to both development mode and advanced compiled versions of your
application and a browser-connected REPL.

This is not a framework. The point of this project is take the raw
materials of Ring, Compojure, Enlive and ClojureScript and show you
how they can be combined to create a great REPL oriented development
experience.

# Getting Started

You will need to have Java, lein and git installed. Execute the following commands
to install both One and ClojureScript:

    git clone git@github.com:brentonashworth/clojurescript-starter-kit.git
    git clone git://github.com/clojure/clojurescript.git
    cd clojurescript
    ./script/bootstrap
    cd ../clojurescript-starter-kit
    lein deps
    ./script/run
    
If you already have ClojureScript and it is not in the same directory as
`clojurescript-starter-kit`, then you will need to set the
CLOJURESCRIPT_HOME environment variable.

Open your browser and navigate to [http://localhost:8080](http://localhost:8080)

For more information, see the [wiki][].

# License

Copyright © 2011 Brenton Ashworth and Relevance, Inc

Distributed under the Eclipse Public License, the same as Clojure uses. See the file COPYING.

[wiki]: https://github.com/brentonashworth/clojurescript-starter-kit/wiki
