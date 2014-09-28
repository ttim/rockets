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
;(defonce world (atom model/start-state))
(defonce world (atom model/game-state))
;(defonce world (atom model/finish-state))

;(util/bind-state-log world (.getElementById js/document "state-log"))

; define render function
(q/defcomponent
  GameComponent [data world]
  (html
    [:div
     [:div {:style {:width (+ game/boards-width rockets.sprites/sprite-width), :margin "0 auto"}}
      (case (:type data)
        :start (start/StartComponent data world)
        :game (game/GameComponent data world)
        :finish (finish/FinishComponent data world))]
     [:div {:id "footer"}
      [:div.titleText {:id "left-footer"} "Awesome Rocketeers"]
      [:div.titleText {:id "right-footer"}
       [:button.button "Share"]
       [:button.button "Vote"]
       ]]]))

(defn render [data]
  (q/render
    (GameComponent data world)
    (.getElementById js/document "main-area")))

; render for first time
(defonce _first_time_render (render @world))

; watch world state and re-render in case of change
(add-watch
  world ::render
  (fn [_ _ _ data] (render data)))

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
      (reset! world (model/tick @world tick))
      (recur (inc tick)))))

; keyboard controller
(def commands (chan))
(keys/init-events! commands (fn [] (= (@world :type) :game)))
(def keys-handler
  (go
    (loop []
      (let [command (<! commands)
            board ({:player1 :board1, :player2 :board2} (command 0))
            action (command 1)
            new-world (case action
                        :rotate (model/rotate @world board)
                        :up (model/move @world board 0)
                        :right (model/move @world board 1)
                        :down (model/move @world board 2)
                        :left (model/move @world board 3)
                        @world)]
        (reset! world new-world))
      (recur))))
