(ns codeclimate.ignore
  "It does nothing, used for testing")

(defn huh []
  (->> ["foo"
        "bar"]
       distinct))

(defn making-it-worse [arg]
  (if (some arg)
    :foo
    nil))

(defn another-mistake [uh]
  (+ 1 uh))