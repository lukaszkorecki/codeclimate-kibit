(ns codeclimate.kibit
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.tools.cli :as cli]
            [kibit.driver :as kibit]
            kibit.check
            [kibit.reporters :as reporters]
            [cheshire.core :as json])
  (:import (java.io StringWriter File))
  (:gen-class))

(defn- debug [fmt-str & args]
  (when (System/getenv "DEBUG")
    (apply printf (apply conj [(str fmt-str "\n")] args))))

(defn pprint-code [form]
  (let [string-writer (StringWriter.)]
    (pp/write form
              :dispatch pp/code-dispatch
              :stream string-writer
              :pretty true)
    (str string-writer)))

(defn template-solution [alt expr]
  (str "<p>Consider using:</p>"
       "<pre>```" (pprint-code alt) "```</pre>"
       "<p>instead of:</p>"
       "<pre>```" (pprint-code expr) "```</pre>"))

(defn codeclimate-reporter
  [check-map]
  (let [{:keys [file line expr alt]} check-map
        issue {:type               "issue"
               :check_name         "kibit/suggestion"
               :description        (str "Non-idiomatic code found in `" (first (seq expr)) "`")
               :categories         ["Style"]
               :location           {:path  (str file)
                                    :lines {:begin line
                                            :end   line}}
               :content            {:body (template-solution alt expr)}
               :remediation_points 50000}]
    (println (str (json/generate-string issue) "\0"))))

(defn target-files
  [config]
  (let [included (map #(io/file %) (get config :include_paths))]
    (let [expanded (flatten (map file-seq included))]
      (map str (filter #(.isFile ^File %) expanded)))))

(defn analyze
  [dir config]
  (let [target-files  (target-files config)]
    (mapv (fn [f]
            (debug "file=%s" f)
             (kibit.check/check-file f
                                   :reporter codeclimate-reporter))
          target-files)))

(def cli-options
  [["-C" "--config PATH" "Load PATH as a config file"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["CodeClimate kibit engine"
        ""
        "Usage: java -jar codeclimate.jar [options] DIR"
        ""
        "Options:"
        options-summary
        ""]
       (clojure.string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (clojure.string/join \newline errors)))

(defn run-checks [arguments options]
  (printf "args=%s opts=%s\n" arguments options)
  (let [target-dir  (io/file (first arguments))
        config-file (io/file (:config options))
        config-data (when (and config-file (.exists config-file))
                      (json/parse-string (slurp config-file) true))]
    (debug "target-dir=%s" target-dir)
    (analyze target-dir config-data)))

(defn exit [status message]
  (println message)
  (System/exit status))

(defn -main [& args]
  (when (System/getenv "DEBUG")
    (println "HIIIIII"))
  (debug "args=%s" args)
  (try
    (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
      (cond
        (:help options) (exit 0 (usage summary))
        (not= (count arguments) 1) (exit 1 (usage summary))
        errors (exit 0 (error-msg errors)))
      (run-checks arguments options))
    (catch Exception e
      (println e))))
