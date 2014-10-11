(ns rockets.core
  (:require
    [figwheel.client :as fw]
    [sablono.core :as html :refer-macros [html]]
    [clojure.string :as string]
    [goog.events :as events]
    [goog.dom :as dom]
    [cljs.core.async :as async :refer [<! >! chan close! sliding-buffer put! alts!]]

    [rockets.state :as state]
    [rockets.util :as util]
    [rockets.keys :as keys]
    [rockets.view :as view]
    [rockets.audio :as audio])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

; world state
(defonce world (atom state/start-state))
;(defonce world (atom state/game-state))
;(defonce world (atom state/finish-state))

; audio
(audio/init! world)

;(util/bind-state-log! world (.getElementById js/document "state-log"))
;(reset! rockets.sprites/debug-sprites? true)

; define render function
(util/render! :main-render world (.getElementById js/document "main-area") #(view/WorldComponent @world world))

; watch world for background painting
(defn update-background [world]
  (let [background-class (if (= (:type world) :finish) "grey-background" "usual-background")]
    (js/console.log background-class)
    (set! (.-className (js/document.getElementById "body")) background-class)))
(update-background @world)
(add-watch
  world ::background-updater
  (fn [_ _ old-data data] (when (not (= (:type old-data) (:type data))) (update-background data))))

; watch js code reload, by this code we are forcing world atom update without update itself
(fw/watch-and-reload
  :jsload-callback
  (fn [] (swap! world update-in [:tmp-dev] not)))

; ticker controller
(def ticker
  (go
    (loop [tick 0]
      (<! (async/timeout 20))
      (reset! world (state/tick @world tick))
      (recur (inc tick)))))

; keyboard controller
(def commands (chan))
;(keys/debug-keys! true)
(keys/init-events! commands (fn [] (= (@world :type) :game)))
(def keys-handler
  (go
    (loop []
      (let [command (<! commands)
            board ({:player1 :board1, :player2 :board2} (command 0))
            action (command 1)
            new-world (case action
                        :rotate (state/rotate @world board)
                        :up (state/move @world board 0)
                        :right (state/move @world board 1)
                        :down (state/move @world board 2)
                        :left (state/move @world board 3)
                        @world)]
        (reset! world new-world))
      (recur))))
