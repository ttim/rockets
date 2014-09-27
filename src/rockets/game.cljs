(ns rockets.game
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]
    [rockets.sprites :as sprites]))

(q/defcomponent
  Sprite []
  (html
    (sprites/sprite)))

(q/defcomponent
  GameComponent [data world-atom]
  (Sprite))
