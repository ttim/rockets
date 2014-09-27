(ns rockets.game
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]
    [rockets.sprites :as sprites]
    [rockets.model :as model]))

(q/defcomponent
  CellComponent [cell selected?]
  (sprites/SpriteComponent))

(q/defcomponent
  FieldComponent [cells selected]
  (html [:div (for [x (range 0 model/size-n)
                    y (range 0 model/size-m)
                    :let [cell (nth (nth cells x) y)]]
                (CellComponent cell false))]))

(q/defcomponent
  BoardComponent [board]
  (FieldComponent (:cells board) (:selected board)))

(q/defcomponent
  RocketsComponent [rockets]
  (sprites/SpriteComponent))

(q/defcomponent
  GameComponent [data world-atom]
  (BoardComponent (:board1 data)))
