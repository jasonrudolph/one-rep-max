CLOJURESCRIPT_HOME=lib/clojurescript

CLJSC_CP='lib/*:lib/dev/*'
for next in 'src/clj' 'src/cljs' 'test/cljs'; do
  CLJSC_CP=${CLJSC_CP}:$CLOJURESCRIPT_HOME'/'$next
done

CLJSC_CP=$CLJSC_CP':lib/domina/src/cljs:src/app/clj:src/app/cljs:src/app/cljs-macros:src/lib/clj:src/lib/cljs:test:templates'
