# One Rep Max

To some extent, One Rep Max is a mobile web app for tracking workout
data. But mostly, it's a playground/opportunity for me to learn and
explore ClojureScript.

## Take it for a spin

One Rep Max logs your workouts, so you can quickly and easily see the
performance from your last workout, and push yourself to reach new
personal records. It's a mobile web app: you take it with you to the gym
and log your sets as you go.

**Mobile first, mobile only** — The UI is designed exclusively for use
on a mobile device. (Since I don't currently have a need for anything
other than a mobile interface, no other interface currently exists.)
It's tested on Safari running on an iPhone on iOS 5. It probably works
on numerous other browsers and platforms, but they're not officially
supported.

You can take it for a spin at
[onerepmax.jasonrudolph.com](http://onerepmax.jasonrudolph.com), or you
can follow the instructions below to [build and
deploy](#building-and-deploying) your own copy of the app.

## Screenshots

[![One Rep Max Screenshot #1 - Welcome](https://raw.github.com/jasonrudolph/one-rep-max/master/doc/screenshots/one-rep-max-1-welcome-medium.png)](https://raw.github.com/jasonrudolph/one-rep-max/master/doc/screenshots/one-rep-max-1-welcome.png "One Rep Max Screenshot #1 - Welcome")
[![One Rep Max Screenshot #2 - Initializing](https://raw.github.com/jasonrudolph/one-rep-max/master/doc/screenshots/one-rep-max-2-initializing-small.png)](https://raw.github.com/jasonrudolph/one-rep-max/master/doc/screenshots/one-rep-max-2-initializing.png "One Rep Max Screenshot #2 - Initializing")
[![One Rep Max Screenshot #3 - Exercise List](https://raw.github.com/jasonrudolph/one-rep-max/master/doc/screenshots/one-rep-max-3-exercise-list-small.png)](https://raw.github.com/jasonrudolph/one-rep-max/master/doc/screenshots/one-rep-max-3-exercise-list.png "One Rep Max Screenshot #3 - Exercise List")
[![One Rep Max Screenshot #4 - Filtering Exercise List](https://raw.github.com/jasonrudolph/one-rep-max/master/doc/screenshots/one-rep-max-4-filtering-exercise-list-small.png)](https://raw.github.com/jasonrudolph/one-rep-max/master/doc/screenshots/one-rep-max-4-filtering-exercise-list.png "One Rep Max Screenshot #4 - Filtering Exercise List")
[![One Rep Max Screenshot #5 - Logging a Set](https://raw.github.com/jasonrudolph/one-rep-max/master/doc/screenshots/one-rep-max-5-logging-a-set-small.png)](https://raw.github.com/jasonrudolph/one-rep-max/master/doc/screenshots/one-rep-max-5-logging-a-set.png "One Rep Max Screenshot #5 - Logging a Set")

## Architecture

One Rep Max is a "single-page application" built on top of
[ClojureScript One][clojurescript-one], with [MongoHQ][] as the backend
data store.

Wikipedia [describes the interaction with a single-page
app][single-page-app] as follows:

> All necessary code – HTML, JavaScript, and CSS – is retrieved with a
> single page load. ... [There is] no page reload by the browser during
> an application session. All user interaction and changes of the
> application state are handled in the context of a single Web document.
> [As a result], the user experience becomes more continuous and fluid...

Each user has his/her own MongoDB database hosted at MongoHQ.  One Rep
Max uses MongoHQ's REST API to fetch and persist user data.  When you
launch One Rep Max, it prompts you to provide your MongoHQ API key and
database name. With that information, One Rep Max is able to access your
database at MongoHQ and use it as the data store for your workouts.

After One Rep Max downloads to your browser, there is no further
communication between your browser and the One Rep Max server; all
subsequent communication flows exclusively between your browser and
api.mongohq.com, all via HTTPS. Your MongoHQ API key is never sent to
the One Rep Max server, nor is any other user data.

## Hacking

### Dependencies

One Rep Max is developed and tested with the following dependencies.

  * Java 1.6
  * Leiningen 1.7.1
  * Git

If you want to change the CSS, you'll also need Ruby and Compass.

  * Ruby 1.9.3-p194
  * Compass 0.12.2

### Getting started

You will need to have Java, [lein][] and Git installed. Execute the
following commands to install and run One Rep Max:

    git clone https://github.com/jasonrudolph/one-rep-max.git
    cd one-rep-max
    lein bootstrap
    lein repl

At the REPL prompt which appears, type `(go)`. Your browser will
launch and navigate to the running application.

You will need an API key and a database for [MongoHQ]. Follow the
instructions in the One Rep Max UI to sign up for a free account and
create your database.

### SCSS, not CSS

One Rep Max uses SCSS and [Compass] to generate the CSS used in the app.

To make changes to the CSS, install Compass and tell it to watch for
changes to the SCSS files:

    gem install compass -v0.12.2
    compass watch

When you change any of the SCSS files (in `src/sass`), Compass will
compile the CSS into the right spot.

### Building and deploying

ClojureScript One provides a [handy script for producing deployment
artifacts][clojurescript-one-build-script].

    ./script/build

The output includes the application's JavaScript compiled in advanced
mode, the host HTML page, and all of the resources in the `public`
directory.

The build script deposits the deployment artifacts in the `./out/public`
directory. Inside you'll find `index.html`: the host page for the
application. You can open `index.html` directly in a browser, and you're
ready to rock.

Since the deployment artifacts are just static content (i.e.,
JavaScript, HTML, etc.), you can deploy the app just about anywhere.
(Heck, you could even [host it on Dropbox][deploy-to-dropbox].)
Personally, I host the app on a simple Apache server, and I use
`./script/deploy` to build the app, deploy it, and then tag the
deployment. If you want to deploy the app to your own host, this script
might serve as a starting point.

### Vim users are people too

If you're a Vim user, you'll want to be able to evaluate ClojureScript
forms from Vim and have them sent to the browser for execution.

1. Install tmux
2. Install [tslime.vim][tslime.vim] and set up your keybindings as
   described in the tslime README
3. Install [lein-repls][lein-repls] and install the `cljsh` script on
   your path
4. Open a tmux session with two panes, each of which is in the root
   project directory
5. In one pane ...
    1. Run `lein repls`
    2. In the REPL prompt which appears, type `(go)`
6. In the other pane ...
    1. Open Vim
    2. Find a form that you want to evaluate and hit Control-c Control-c
    3. You'll be prompted to identify the tmux session, window, and pane
       where you ran `lein repls`. Do so, and watch the magic happen.

For more info on working with Vim and ClojureScript, check out the
[ClojureScript wiki][clojurescript-with-vim]. (The steps above are a
subset of the steps described in the wiki, but this is all you need for
evaluating *ClojureScript* code from within Vim.)

## TODO

* Provide visual feedback to the user (i.e., spinner or equivalent) when
  the app is working.
* Validate user input. (The ClojureScript One sample app provides an
  [example][clojurescript-one-form-state].)
* Support navigation with the browser's "back" button. See:
  [one.sample.history][history-management].
* Compile compressed CSS as part of the build (using `compass compile
  --output-style=compressed`) and remove the generated stylesheet from
  the repo (i.e., `screen.css`).

## Back up your data

If you use One Rep Max for real production data (i.e., you use it to
track your workouts and you care about not losing your data), be sure to
set up automatic backups for your data. You can use the [backup
services provided by MongoHQ][mongohq-backup] or you can roll your own.

## Credits

* Thanks to [Brenton Ashworth][brentonashworth] and others at
  [Relevance] for ClojureScript One.
* Thanks to [Kevin Altman][itsthatguy] and [Jamie Kite][jgkite] for
  patiently donating their UI design skills to the app.
* Thanks to [P.J. Onori][somerandomdude] for [Iconic][], which provides
  the icons used in One Rep Max. (Iconic is distributed under a
  [Creative Commons license][iconic-license].)
* Thanks to Relevance for [20% time][relevance-fridays], where much of
  this work took place.
* Thanks to [Brenton Ashworth][brentonashworth] and [Stuart
  Sierra][stuartsierra] for entertaining countless questions and
  providing valuable feedback as I explored the ClojureScript landscape.

## Open source, but not an "open source project"

"I am sharing my code. I am not launching an open source project." --
Alan Gutierrez

[http://kiloblog.com/post/sharing-code-for-what-its-worth](http://kiloblog.com/post/sharing-code-for-what-its-worth "GitHub and Git: Sharing Your Code, for What It's Worth ...")

## License

Copyright 2012 Jason Rudolph ([jasonrudolph.com](http://jasonrudolph.com)) and Relevance ([thinkrelevance.com](http://thinkrelevance.com)).

Distributed under the Eclipse Public License, the same as Clojure uses. See the file COPYING.

[brentonashworth]: https://github.com/brentonashworth
[clojurescript-one]: http://clojurescriptone.com
[clojurescript-one-build-script]: https://github.com/brentonashworth/one/wiki/Building-deployment-artifacts
[clojurescript-one-form-state]: https://github.com/jasonrudolph/one-rep-max/blob/6129d57/doc/interactions.png
[clojurescript-with-vim]: https://github.com/clojure/clojurescript/wiki/Vim
[compass]: http://compass-style.org/
[deploy-to-dropbox]: http://www.maclife.com/article/howtos/how_host_your_website_dropbox
[history-management]: https://github.com/jasonrudolph/one-rep-max/blob/21099b6/src/app/cljs/one/sample/history.cljs
[iconic]: http://somerandomdude.com/work/iconic/
[iconic-license]: https://github.com/jasonrudolph/one-rep-max/blob/master/public/fonts/iconic_license.txt
[itsthatguy]: https://github.com/itsthatguy
[jgkite]: https://github.com/jgkite
[lein]: https://github.com/technomancy/leiningen
[lein-repls]: https://github.com/franks42/lein-repls
[mongohq]: https://mongohq.com
[mongohq-backup]: http://support.mongohq.com/topics/using-amazon-s3-to-backup-your-mongohq-database.html
[relevance]: http://thinkrelevance.com
[relevance-fridays]: http://thinkrelevance.com/how-we-work/dev_team#dev_team-fridays
[single-page-app]: http://en.wikipedia.org/wiki/Single-page_application
[somerandomdude]: https://github.com/somerandomdude
[stuartsierra]: https://github.com/stuartsierra
[tslime.vim]: https://github.com/jgdavey/tslime.vim
