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

(defn convert
  ([progress min-value max-value] (convert progress 0 100 min-value max-value))
  ([progress min-progress max-progress min-value max-value] (+ min-value (/ (* (- progress min-progress) (- max-value min-value)) (- max-progress min-progress)))))

; coordinates as school axis
(defn calc-x-coordinate
  [player slot]
  (let [offset (if (= player :player1) 0 (+ board-width space-between-boards))]
    (+ sprites/sprite-width offset (* slot sprites/sprite-width))))

(defn calc-flying-rocket-coordinates [rocket]
  (let [source-player (:source-player rocket)
        target-player ({:player1 :player2, :player2 :player1} source-player)
        source-x (calc-x-coordinate source-player (:source-slot rocket))
        target-x (calc-x-coordinate target-player (:target-slot rocket))
        progress (:progress rocket)                         ; 0..20 20..80 80..100
        y-progress (if (<= progress 20) (* progress 5) (if (<= progress 80) 100 (* (- 100 progress) 5)))
        x-progress (if (<= progress 20) 0 (if (<= progress 80) (convert progress 20 80 0 100) 100))]
    {:x (convert x-progress source-x target-x), :y (convert y-progress 0 (- rockets-space-height (+ sprites/rocket-height 16)))}))

(defn calc-rocket-coordinates [rocket]
  (let [source-player (:source-player rocket)
        source-slot (:source-slot rocket)
        progress (:progress rocket)]                        ;0-100
    (case (rocket :state)
      :staying {:x (calc-x-coordinate source-player source-slot), :y 0}
      :dying {:x (calc-x-coordinate source-player source-slot), :y (convert progress 0 rockets-space-height)}
      :flying (calc-flying-rocket-coordinates rocket))))

(q/defcomponent
  RocketComponent [rocket]
  (let [fire? (not (= (:state rocket) :staying))
        coordinates (calc-rocket-coordinates rocket)
        fuel (:fuel rocket)
        state (:state rocket)
        progress (:progress rocket)
        height (if (= state :dying) (- rockets-space-height (convert progress 0 rockets-space-height)) sprites/rocket-height)]
    (html
      [:div {:style {:position "absolute", :bottom (:y coordinates), :left (:x coordinates)}}
       (sprites/RocketComponent {:fire? fire? :fuel fuel :height height})])))

(q/defcomponent
  RocketsComponent [rockets]
  (html
    [:div {:style {:position "absolute", :width boards-width, :height rockets-space-height}}
     (map #(RocketComponent %) rockets)]))

(q/defcomponent
  PlayerNameComponent [name]
  (html
    [:h1 name]))

(q/defcomponent
  PlayerNamesComponent [args]
  (html
    [:div {:style {:position "absolute", :width boards-width, :top (- rockets-space-height (* 5 sprites/sprite-width))}}
     [:div {:style {:position "absolute", :width (- board-width sprites/sprite-width), :left sprites/sprite-width}}
      (PlayerNameComponent (:player1 args))]
     [:div {:style {:position "absolute", :width (- board-width sprites/sprite-width), :left (+ board-width space-between-boards sprites/sprite-width)}}
      (PlayerNameComponent (:player2 args))]]))

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
    [:div {:style {:width boards-width, :height (+ board-height rockets-space-height)}}
     [:div {:style {:position "absolute", :top (+ rockets-space-height 7)}}
      [:table {:style util/no-borders-style}
       [:tr {:style util/no-borders-style}
        [:td {:style util/no-borders-style} (BoardComponent (:board1 data))]
        [:td {:style (merge {:width space-between-boards} util/no-borders-style)}]
        [:td {:style util/no-borders-style} (BoardComponent (:board2 data))]]
       [:tr {:style util/no-borders-style} (FitilComponent)]]]
     [:div {:style {:position "absolute"}} (RocketsComponent (:rockets data))]
     [:div {:style {:position "absolute"}} (PlayerNamesComponent data)]]))
