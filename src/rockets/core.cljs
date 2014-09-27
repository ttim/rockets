(ns rockets.core
  (:require
    [figwheel.client :as fw]
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.model :as model]
    [rockets.util :as util]

    [rockets.start :as start]
    [rockets.game :as game]
    [rockets.finish :as finish]
    ))

; world state
(defonce world (atom model/game-state))

(util/bind-state-log world (.getElementById js/document "state-log"))

; define render function
(defn render [data]
  (q/render
    (case (:type data)
      :start (start/StartComponent data world)
      :game (game/GameComponent data world)
      :finish (finish/FinishComponent data world))
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
