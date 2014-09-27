(ns rockets.core
  (:require
    [figwheel.client :as fw]
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.model :as model]
    [rockets.util :as util]
    [goog.events :as events]
    [goog.dom :as dom]
    [cljs.core.async :as async :refer [<! >! chan close! sliding-buffer put! alts!]]

    [rockets.keys :as keys]
    [rockets.start :as start]
    [rockets.game :as game]
    [rockets.finish :as finish])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

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

; ticker controller
(def ticker
  (go
    (loop [tick 0]
      (<! (async/timeout 10))
      (reset! world (model/event-tick @world tick))
      (recur (inc tick)))))

; keyboard controller
(def commands (chan))
(keys/init-events! commands)
(def keys-handler
  (go
    (loop []
      (let [command (<! commands)
            board ({:player1 :board1, :player2 :board2} (command 0))
            action (command 1)
            new-world (case action
                        :rotate (model/event-select @world board)
                        :up (model/event-move-selection @world board 0)
                        :right (model/event-move-selection @world board 1)
                        :down (model/event-move-selection @world board 2)
                        :left (model/event-move-selection @world board 3)
                        @world)]
        (reset! world new-world))
      (recur))))
