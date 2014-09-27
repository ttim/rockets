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
  (sprites/CoolSpriteComponent (:cell-type cell) (:orientation cell)))

(q/defcomponent
  FieldComponent [cells selected]
  (html [:table
         {:style util/no-borders-style}
         (for [x (range 0 model/size-n)]
           [:tr {:style util/no-borders-style}
            (for [y (range 0 model/size-m)
                  :let [cell (nth (nth cells x) y)]]
              [:td {:style util/no-borders-style} (CellComponent cell false)])])]))

(q/defcomponent
  BoardComponent [board]
  (FieldComponent (:cells board) (:selected board)))

(q/defcomponent
  RocketsComponent [rockets]
  (sprites/SpriteComponent))

(q/defcomponent
  GameComponent [data world-atom]
  (BoardComponent (:board1 data)))
