(ns rockets.game
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]
    [rockets.sprites :as sprites]))

(q/defcomponent
  CellComponent [cell selected?]
  (sprites/SpriteComponent))

(q/defcomponent
  FieldComponent [cells selected]
  (CellComponent))

(q/defcomponent
  BoardComponent [board]
  (FieldComponent (:cells board) (:selected board)))

(q/defcomponent
  RocketsComponent [rockets]
  (Sprite))

(q/defcomponent
  GameComponent [data world-atom]
  (BoardComponent (:board1 data)))
