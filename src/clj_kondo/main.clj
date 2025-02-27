(ns clj-kondo.main
  {:no-doc true}
  (:gen-class)
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.core :as core-impl]
   [clj-kondo.impl.profiler :as profiler]
   [clojure.string :as str
    :refer [starts-with?
            ends-with?]]))

(set! *warn-on-reflection* true)

;;;; printing

(defn- print-version []
  (println (str "clj-kondo v" core-impl/version)))

(defn- print-help []
  (print-version)
  (println (format "
Usage: [ --help ] [ --version ] [ --lint <files> ] [ --lang (clj|cljs) ] [ --cache [ <dir> ] ] [ --config <config> ]

Options:

  --lint: a file can either be a normal file, directory or classpath. In the
    case of a directory or classpath, only .clj, .cljs and .cljc will be
    processed. Use - as filename for reading from stdin.

  --lang: if lang cannot be derived from the file extension this option will be
    used.

  --cache: if dir exists it is used to write and read data from, to enrich
    analysis over multiple runs. If no value is provided, the nearest .clj-kondo
    parent directory is detected and a cache directory will be created in it.

  --config: config may be a file or an EDN expression. See
    https://cljdoc.org/d/clj-kondo/clj-kondo/%s/doc/configuration.
" core-impl/version))
  nil)

;;;; parse command line options

(defn- parse-opts [options]
  (let [opts (loop [options options
                    opts-map {}
                    current-opt nil]
               (if-let [opt (first options)]
                 (if (starts-with? opt "--")
                   (recur (rest options)
                          (assoc opts-map opt [])
                          opt)
                   (recur (rest options)
                          (update opts-map current-opt conj opt)
                          current-opt))
                 opts-map))
        default-lang (when-let [lang-opt (first (get opts "--lang"))]
                       (keyword lang-opt))
        cache-opt (get opts "--cache")]
    {:lint (get opts "--lint")
     :cache (when cache-opt
              (or (first cache-opt) true))
     :lang default-lang
     :config (first (get opts "--config"))
     :version (get opts "--version")
     :help (get opts "--help")}))

(defn main
  [& options]
  (try
    (profiler/profile
     :main
     (let [{:keys [:help :lint :version] :as parsed}
           (parse-opts options)]
       (or (cond version
                 (print-version)
                 help
                 (print-help)
                 (empty? lint)
                 (print-help)
                 :else (let [{:keys [:summary]
                              :as results} (clj-kondo/run! parsed)
                             {:keys [:error :warning]} summary]
                         (clj-kondo/print! results)
                         (cond (pos? error) 3
                               (pos? warning) 2
                               :else 0)))
           0)))
    (finally
      (profiler/print-profile :main))))

(defn -main [& options]
  (let [exit-code
        (try (apply main options)
             (catch Throwable e
               (if core-impl/dev? (throw e)
                   (do
                     ;; can't use clojure.stacktrace here, due to
                     ;; https://dev.clojure.org/jira/browse/CLJ-2502
                     (println "Unexpected error. Please report an issue.")
                     (.printStackTrace e)
                     ;; unexpected error
                     124))))]
    (flush)
    (System/exit exit-code)))

;;;; Scratch

(comment
  )
