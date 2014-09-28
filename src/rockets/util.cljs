(ns rockets.util
  (:require
    [clojure.browser.dom :as dom]))

(defn update-text
  [world-atom key value] (reset! world-atom (assoc @world-atom key value)))

(defn update-state-log
  [state element] (dom/set-text element (sablono.util/to-str state)))
(defn bind-state-log
  [state-atom element]
  (add-watch
    state-atom ::state-log-render
    (fn [_ _ _ data] (update-state-log data element)))
  (defonce _first_time_log_render (update-state-log @state-atom element)))

(def no-borders-style {:border-width 0, :padding "0 0 0 0", :margin "0 0 0 0", :border-spacing 0})

(defn redirect-to [url]
  (set! (.-href js/window.location) url))
