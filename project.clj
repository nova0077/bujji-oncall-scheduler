(defproject bujji "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring/ring-core "1.9.5"]
                 [ring/ring-jetty-adapter "1.9.5"]
                 [ring/ring-json "0.5.1"]
                 [ring-cors "0.1.13"]
                 [compojure "1.6.2"]
                 [com.taoensso/timbre "5.2.1"]
                 [lurodrigo/firestore-clj "1.2.1"]
                 [metosin/malli "0.16.4"]
                 [lurodrigo/firestore-clj "1.2.1" :exclusions [com.google.errorprone/error_prone_annotations]]
                 [camel-snake-kebab "0.4.3"]  ; For converting "this" to :this with firestore
                 [environ "1.2.0"]
                 [cheshire "5.13.0"]
                 [clojure.java-time "0.3.2"]]
  :repl-options {:init-ns bujji.core}
  :main bujji.core
  :plugins [[lein-environ "1.2.0"]
            [lein-localrepo "0.5.3"]
            [lein-ancient "0.6.3"]
            [com.jakemccrary/lein-test-refresh "0.10.0"]
            [refactor-nrepl "2.5.0"]
            [cider/cider-nrepl "0.26.0"]]

  :profiles {:dev {:env {:firestore-credentials-file "/Users/praveen/desktop/code/projects/clojure-projects/bujji/resources/service_accountkey.json"}}})
