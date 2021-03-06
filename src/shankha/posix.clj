(ns shankha.posix
  (:import java.io.FileOutputStream
           (org.jruby.ext.posix POSIXFactory
                                POSIXHandler))
  (:require [clojure.java.io :refer (reader)]))


(defn throwf [& message]
  (throw (Exception. (apply format message))))

(def handler
  (proxy [POSIXHandler]
    []
    (error [error extra]
      (println "error:" error extra))
    (unimplementedError [methodname]
      (throwf "unimplemented method %s" methodname))
    (warn [warn-id message & data]
      (println "warning:" warn-id message data))
    (isVerbose []
      false)
    (getCurrentWorkingDirectory []
      (System/getProperty "user.dir"))
    (getEnv []
      (map str (System/getenv)))
    (getInputStream []
      System/in)
    (getOutputStream []
      System/out)
    (getErrorStream []
      System/err)
    (getPID []
      (rand-int 65536))))

(def C (POSIXFactory/getPOSIX handler true))

(defn closeDescriptors []
  (.close System/out)
  (.close System/err)
  (.close System/in))

(defn is-daemon? []
  (System/getProperty "leiningen.daemon"))

(defn cd [directory]
  (.chdir C directory)
  (System/setProperty "user.dir" directory))

(defn get-current-pid []
  (.getpid C))

(defn write-pid-file
  "Write the pid of the current process to pid-path"
  [pid-path]
  (let [pid (str (get-current-pid))]
    (printf "writing pid %s to %s\n" pid pid-path)
    (spit pid-path pid)))

(defn abort
  "Abort, once we're in the user's project, so leiningen.core.main/abort isn't available"
  [message]
  (println message)
  (System/exit 1))

(defn init
  "do all the post-fork setup. set session id, close file descriptors, write pid file"
  [pid-path & {:keys [debug]}]
  ;; (.setsid C)
  ;; (when (not debug)
  ;;   (closeDescriptors))
  (write-pid-file pid-path))

(defn sigterm [pid]
  (.kill C pid 15))
