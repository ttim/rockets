(ns rockets.core
  (:require
    [figwheel.client :as fw]
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.model_sample :as sample]
    [clojure.browser.dom :as cljsdom]
    [rockets.start :as start]))

; world state
(defonce world (atom sample/start-state))

(defn update-text
  [world-atom key value] (reset! world-atom (assoc @world-atom key value)))

; log state
(def show-state-log true)

(defn update-state-log
  [data] (cljsdom/set-text (.getElementById js/document "state-log") (sablono.util/to-str data)))
(when show-state-log
  (add-watch
    world ::state-log-render
    (fn [_ _ _ data] (update-state-log data)))
  (defonce _first_time_log_render (update-state-log @world)))

; define render function
(defn render [data]
  (q/render
    (start/StartComponent data world)
    #_(if (= (:type data) :start) (start/StartComponent data))
    (.getElementById js/document "main-area")))

; render for first time
(defonce _first_time_render (render @world))

; watch world state and re-render in case of change
(add-watch
  world ::render
  (fn [_ _ _ data] (render data)))

; watch js code reload, by this code we are forcing world atom update without update itself
(fw/watch-and-reload
  :jsload-callback
  (fn [] (swap! world update-in [:tmp-dev] not)))
