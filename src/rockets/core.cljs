(ns rockets.core
  (:require
    [figwheel.client :as fw]
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.model_sample :as sample]
    [rockets.start :as start]
    [rockets.util :as util]))

; world state
(defonce world (atom sample/start-state))

(util/bind-state-log world (.getElementById js/document "state-log"))

(q/defcomponent
  DumbComponent [data world-atom]
  (html
    [:h1 "Not Impplemented"]
    ))

; define render function
(defn render [data]
  (q/render
    (if (= (:type data) :start) (start/StartComponent data world) (DumbComponent data world))
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
