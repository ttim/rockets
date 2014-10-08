(ns rockets.game
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]
    [rockets.sprites :as sprites :refer [create-sprites, merge-sprites, sprite-width, add-zone]]
    [rockets.state :as state]))

(def sprites-between-boards 4)
(def rockets-space-sprites-height 6)
(def board-sprites-height (inc state/size-n))
(def board-sprites-width (inc state/size-m))
(def boards-sprites-width (+ sprites-between-boards (* 2 board-sprites-width)))
(def keys-zone-sprites-height 2)
(def player-name-sprite-offset-from-board 5)
(def gamezone-height (+ rockets-space-sprites-height board-sprites-height keys-zone-sprites-height))

; game-zone sprites
(defn field-sprites [cells selected]
  (create-sprites
    state/size-n state/size-m
    #(let [x (- state/size-n (inc %1))
           y %2
           cell ((cells x) y)]
      (sprites/CoolSpriteComponent {:type      (:cell-type cell),
                                    :angle     (:orientation cell),
                                    :selected? (and (= x (:x selected)) (= y (:y selected))),
                                    :fire?     (:locked cell)}))))

(defn board-sprites [board]
  (let [selected (board :selected)
        shuffle-selected? (and (= (selected :x) 0) (= (selected :y) -1))]
    (-> (create-sprites board-sprites-height board-sprites-width)
        (merge-sprites 0 1 (field-sprites (:cells board) selected))
        (merge-sprites (dec state/size-n) 0 [[(sprites/ShuffleComponent (assoc board :selected? shuffle-selected?))]])
        (merge-sprites state/size-n 1 (create-sprites 1 state/size-m (fn [x y] (sprites/FireComponent)))))))

(defn boards-sprites [boards]
  (-> (create-sprites board-sprites-height boards-sprites-width)
      (merge-sprites 0 0 (board-sprites (:board1 boards)))
      (merge-sprites 0 (+ sprites-between-boards (inc state/size-m)) (board-sprites (:board2 boards)))))

; rockets
(def rockets-space-height (* sprite-width rockets-space-sprites-height))

(defn convert
  ([progress min-value max-value] (convert progress 0 100 min-value max-value))
  ([progress min-progress max-progress min-value max-value] (+ min-value (/ (* (- progress min-progress) (- max-value min-value)) (- max-progress min-progress)))))

; coordinates as school axis
(defn calc-x-coordinate
  [player slot]
  (let [offset (if (= player :player1) 0 (+ board-sprites-width sprites-between-boards))]
    (* sprite-width (+ 1 offset slot))))

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
    [:div {:style {:position "absolute", :width "100%", :height "100%"}}
     (map #(RocketComponent %) rockets)]))

(q/defcomponent
  HeaderComponent [name]
  (html
    [:h1 {:style (assoc sprites/names-style :text-align "center")} name]))

(defn gamezone-sprites [data]
  (let [keys-offset-x (+ rockets-space-sprites-height board-sprites-height)
        players-name-offset-x (- rockets-space-sprites-height player-name-sprite-offset-from-board)
        player1-offset-y 1
        player2-offset-y (+ 1 board-sprites-width sprites-between-boards)]
    (-> (create-sprites gamezone-height boards-sprites-width)
        (merge-sprites rockets-space-sprites-height 0 (boards-sprites data))
        (add-zone keys-offset-x player1-offset-y state/size-n 2 (HeaderComponent "W S A D + Q"))
        (add-zone keys-offset-x player2-offset-y state/size-n 2 (HeaderComponent "Arrows + Space"))
        (add-zone players-name-offset-x player1-offset-y state/size-n 2 (HeaderComponent (:player1 data)))
        (add-zone players-name-offset-x player2-offset-y state/size-n 2 (HeaderComponent (:player2 data)))
        (add-zone 0 0 boards-sprites-width rockets-space-sprites-height (RocketsComponent (:rockets data))))))

(q/defcomponent GameComponent [data world-atom] (sprites/SpritesComponent (gamezone-sprites data)))
