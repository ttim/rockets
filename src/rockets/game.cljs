(ns rockets.game
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]
    [rockets.sprites :as sprites]
    [rockets.model :as model]))

(q/defcomponent
  ;CellComponent [cell selected?]
  CellComponent [args]
  (let [cell (args :cell)]
    (sprites/CoolSpriteComponent {:type      (cell :cell-type),
                                  :angle     (cell :orientation),
                                  :selected? (args :selected?),
                                  :fire?     (cell :locked)})))

(q/defcomponent
  ;FieldComponent [cells selected]
  FieldComponent [args]
  (html [:table {:style util/no-borders-style}
         (for [x (reverse (range 0 model/size-n))]
           [:tr {:style util/no-borders-style}
            (for [y (range 0 model/size-m)
                  :let [cell (nth (nth (args :cells) x) y)]]
              [:td {:style util/no-borders-style}
               (CellComponent {:cell cell, :selected? (and (= x ((args :selected) :x)) (= y ((args :selected) :y)))})])])]))

(q/defcomponent
  BoardComponent [board]
  (let [selected (board :selected)
        shuffle-selected? (and (= (selected :x) 0) (= (selected :y) -1))]
    (do
      (html [:table {:style util/no-borders-style}
             [:tr {:style util/no-borders-style}
              [:td {:style (merge {:vertical-align "bottom"} util/no-borders-style)}
               (sprites/ShuffleComponent (assoc board :selected? shuffle-selected?))]
              [:td {:style util/no-borders-style} (FieldComponent board)]]])
      )
    ))

(def space-between-boards (* sprites/sprite-width 4))
(def board-width (* sprites/sprite-width (inc model/size-m)))
(def boards-width (+ board-width board-width space-between-boards))
(def board-height (* sprites/sprite-width (inc model/size-n)))
(def rockets-space-height (* sprites/sprite-width 6))

; coordinates as school axis
(defn calc-rocket-coordinates [rocket]
  (let [source-player (:source-player rocket)
        source-slot (:source-slot rocket)
        offset (if (= source-player :player1) 0 (+ board-width space-between-boards))
        progress (:progress rocket)]                        ;0-100
    (case (rocket :state)
      :staying {:x (+ sprites/sprite-width offset (* source-slot sprites/sprite-width)), :y 0}
      :dying {:x (+ sprites/sprite-width offset (* source-slot sprites/sprite-width)), :y (* progress 1)}
      :flying {:x (+ sprites/sprite-width offset (* source-slot sprites/sprite-width)), :y 0})))

(q/defcomponent
  RocketComponent [rocket]
  (let [fire? (not (= (:state rocket) :staying))
        coordinates (calc-rocket-coordinates rocket)
        fuel (:fuel rocket)
        state (:state rocket)
        height (if (= state :dying) 30 sprites/rocket-height)]
    (html
      [:div {:style {:position "absolute", :bottom (:y coordinates), :left (:x coordinates)}}
       (sprites/RocketComponent {:fire? fire? :fuel fuel :height height})])))

(q/defcomponent
  RocketsComponent [rockets]
  (html
    [:div {:style {:position "absolute", :width boards-width, :height rockets-space-height}}
     (map #(RocketComponent %) rockets)]))

(q/defcomponent
  FitilComponent []
  (html [:table {:style util/no-borders-style}
         [:tr {:style util/no-borders-style}
          [:td {:style (merge {:width sprites/sprite-width} util/no-borders-style)}]
          (repeat model/size-m [:td {:style util/no-borders-style} (sprites/FireComponent)])
          [:td {:style (merge {:width space-between-boards} util/no-borders-style)}]
          [:td {:style (merge {:width sprites/sprite-width} util/no-borders-style)}]
          (repeat model/size-m [:td {:style util/no-borders-style} (sprites/FireComponent)])]]))

(q/defcomponent
  GameComponent [data world-atom]
  (html
    [:div {:style {:width (+ boards-width sprites/sprite-width), :height (+ board-height rockets-space-height), :background-color "#333355"}}
     [:div {:style {:position "absolute", :top (+ rockets-space-height 7)}}
      [:table {:style util/no-borders-style}
       [:tr {:style util/no-borders-style}
        [:td {:style util/no-borders-style} (BoardComponent (:board1 data))]
        [:td {:style (merge {:width space-between-boards} util/no-borders-style)}]
        [:td {:style util/no-borders-style} (BoardComponent (:board2 data))]]
       [:tr {:style util/no-borders-style} (FitilComponent)]]]
     [:div {:style {:position "absolute"}} (RocketsComponent (:rockets data))]]))
