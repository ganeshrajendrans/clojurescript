;; Copyright (c) Rich Hickey. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns cljs.repl.bootstrap
  (:require [cljs.repl :as repl]))

(defn install-repl-goog-require
  "Install a version of goog.require that supports namespace reloading.
   IMPORTANT: must be invoked *after* loading cljs.core."
  [repl-env env]
  (repl/evaluate-form repl-env env "<cljs repl>"
    '(set! (.-require__ js/goog) js/goog.require))
  ;; monkey-patch goog.require
  (repl/evaluate-form repl-env env "<cljs repl>"
    '(set! (.-require js/goog)
       (fn [src reload]
         (when (= reload "reload-all")
           (set! (.-cljsReloadAll_ js/goog) true))
         (let [reload? (or reload (.-cljsReloadAll__ js/goog))]
           (when reload?
             ;; check for new-ish private goog/debugLoader
             (if (some? goog/debugLoader_)
               (let [path (.getPathFromDeps_ goog/debugLoader_ name)]
                 (goog.object/remove (.-written_ goog/debugLoader_) path)
                 (goog.object/remove (.-written_ goog/debugLoader_)
                   (str js/goog.basePath path)))
               ;; legacy approach
               (let [path (goog.object/get js/goog.dependencies_.nameToPath src)]
                 (goog.object/remove js/goog.dependencies_.visited path)
                 (goog.object/remove js/goog.dependencies_.written path)
                 (goog.object/remove js/goog.dependencies_.written
                   (str js/goog.basePath path)))))
           (let [ret (.require__ js/goog src)]
             (when (= reload "reload-all")
               (set! (.-cljsReloadAll_ js/goog) false))
             ret))))))