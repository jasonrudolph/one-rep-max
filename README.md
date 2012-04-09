# One Rep Max

TODO Add background info:

* Built using [ClojureScript One][clojurescript-one]
* Uses MongoHQ as the backend
* Tested on Mobile Safari running on an iPhone on iOS 5. (It probably
  works on numerous other browsers and platforms, but they're not
  officially supported. Explain why.)

# Open source, but not an "open source project"

"I am sharing my code. I am not launching an open source project." --
Alan Gutierrez

[http://kiloblog.com/post/sharing-code-for-what-its-worth](http://kiloblog.com/post/sharing-code-for-what-its-worth "GitHub and Git: Sharing Your Code, for What It's Worth ...")

# Getting started

You will need to have Java, [lein][] and Git installed. Execute the
following commands to install and run One Rep Max:

    git clone https://github.com/jasonrudolph/one-rep-max.git
    cd one
    lein bootstrap
    lein repl

At the REPL prompt which appears, type `(go)`. Your browser will
launch and navigate to the running application.

You will also need an API key and a database for MongoHQ:

1. Sign up for a free account at [mongohq.com][mongohq-signup].
2. [Create a database][mongohq-create-db] named "one-rep-max". (MongoHQ
   offers several options when creating a database. The free "Sandbox
   Database" will likely meet your needs for this app.)
3. Find your API key in the "My Account" section of mongohq.com. 
4. Until the UI supports the ability to create exercises, you can use
   the `seed` script to create a few exercises for testing purposes:

       ./script/seed <your-api-key>

# Back up your data

If you use One Rep Max for real production data (i.e., you use it to
track your workouts and you care about not losing your data), be sure to
set up automatic backups for your data. You can use the [backup
services provided by MongoHQ][mongohq-backup] or your can roll your own.

# Credits

* Thanks to @brentonashworth and others at @relevance for ClojureScript
  One and for entertaining countless questions as I explored the
  ClojureScript landscape.
* Thanks to @relevance for [Fridays], where much of this work took
  place.

# License

Copyright 2012 Jason Rudolph ([jasonrudolph.com](http://jasonrudolph.com)) and Relevance ([thinkrelevance.com](http://thinkrelevance.com)).

Distributed under the Eclipse Public License, the same as Clojure uses. See the file COPYING.

[clojurescript-one]: http://clojurescriptone.com
[lein]: https://github.com/technomancy/leiningen
[mongohq-signup]: https://mongohq.com/signup
[mongohq-create-db]: https://mongohq.com/databases/new
[mongohq-backup]: http://support.mongohq.com/topics/using-amazon-s3-to-backup-your-mongohq-database.html
[fridays]: http://thinkrelevance.com/how-we-work/dev_team#dev_team-fridays
