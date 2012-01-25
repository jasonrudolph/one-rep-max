# ClojureScript One

We think that [ClojureScript][] is amazing and that it provides endless
possibilities for making web development much more fun. To realize
these possibilities, we need your help. Someone has to do the work to
turn the possible into reality.

We think that the best way to convince you to help us is to allow you
to experience the wonder of ClojureScript development for
yourself. The purpose of this project is to help you get started with
ClojureScript as smoothly and quickly as possible and to set you up
with an excellent working environment.

Getting started means many things. We often forget how much we need to
know to be comfortable in a programming environment. How do we
organize code? How do we test? What is the most productive workflow?
How do we deploy our application? We will attempt to answer all of these
questions and provide working examples.

The project includes a working sample application, useful tools, and
libraries in various states of development. Many of the libraries in
the project will eventually mature and become their own projects. The
process of creating good libraries and frameworks takes time and
experience. Instead of waiting until they are finished, we thought it
would be better to show you now. Once you see the potential, we know
that you will want to join in and help.

# What's here

This project will help you lean how to:

* use ClojureScript tools and other Clojure libraries to create a
  productive development environment
* effectively work with the ClojureScript tools
* organize your code
* keep visual design activities separate from application code
* run ClojureScript (in a remote JavaScript environment) from Clojure
* test ClojureScript using any Clojure test framework
* structure a ClojureScript application
* use Clojure data to talk to a Clojure service
* build and deploy to Heroku
* work with ClojureScript dependencies

# Is this a library or a framework?

ClojureScript One is hard to classify. It is not a library or a
framework. It is more like a classroom, a laboratory or a starter
kit. Frameworks limit you to a specific way of thinking. Libraries
attempt to do something for you. We hope that this project will help
you to think of things that no one has ever thought of and empower you
to do things that you may not have thought possible. But most of all,
we hope that it will show you how much fun web development can be in
ClojureScript.

The intended use of the project is:

1. Get all the tools running
2. Read through the wiki, running all the examples
3. Fire up a browser-connected REPL and explore the sample application
3. Use this project as a starting point for your own applications
4. Contribute what you have learned back to this project

# Getting started

You will need to have Java, [lein][] and git installed. Execute the
following commands to install and run One:

    git clone https://github.com/brentonashworth/one.git
    cd one
    lein bootstrap
    lein repl

At the REPL prompt which appears, type `(go)`. Your browser will
launch and navigate to the running application.

Once you have this running, see the [wiki][] and the [website][] for
more information.

# Getting Help

The best place to get help is on the
[Clojure Mailing List](https://groups.google.com/group/clojure). You
can also log issues in the [project issue tracker][issues].

# Contributing

ClojureScript One welcomes help from the community in the form of pull
requests, [bug reports][issues], [wiki][wiki] updates, and hugs. If
you wish to contribute code, please read [How We Work][how-we-work].
In particular, note that pull requests should target the
`pull-requests` branch, not `master`.

# Known Issues

* ClojureScript One supports developing under Windows if you are using
  Chrome, Firefox, or IE9. Versions of Internet Explorer previous to 9
  are not supported at this time.
* Everything on the
  [project issues list](https://github.com/brentonashworth/one/issues).

# One last rant

ClojureScript is designed to make client-service
applications. Traditional web applications run mostly on a server with
a small amount of UI code running on the client. The problem with this
kind of application is that there is a big giant network right in the
middle of your application. We have learned how to deal with this so
well that we actually think this is a good way to write software. There
are many applications for which this is a good approach. But there are
also many applications which would be better as client-service applications
where the entire application runs in the client and uses backend services
which can easily be thought of as other applications.

The reason we haven't used the right tool for the job in the past is
because it was much harder than it should have been. For Clojure
developers, ClojureScript has changed this. ClojureScript allows us to
write very large applications that run on any JavaScript platform. Not
only can we do it, but the experience is better than any other
environment can offer, even JavaScript.

ClojureScript allows us to connect to and modify running
applications, communicate with the server using only Clojure data, run
ClojureScript code in the browser from the server, use protocols to
make existing JavaScript play nice and much, much more.

# License

Copyright © 2012 Brenton Ashworth and Relevance, Inc

Distributed under the Eclipse Public License, the same as Clojure uses. See the file COPYING.

[ClojureScript]: https://github.com/clojure/clojurescript
[lein]: https://github.com/technomancy/leiningen
[wiki]: https://github.com/brentonashworth/one/wiki
[website]: http://clojurescriptone.com
[how-we-work]: https://github.com/brentonashworth/one/wiki/HowWeWork
[issues]: https://github.com/brentonashworth/one/issues
