(ns rockets.game
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]
    [rockets.sprites :as sprites]
    [rockets.state :as state]))

(q/defcomponent
  SpritesComponent [sprites]
  (html [:div {:style {:border "1px double white"}}
         [:table {:style util/no-borders-style}
          (for [sprites-line sprites]
            [:tr {:style util/no-borders-style}
             (for [sprite sprites-line]
               [:td {:style util/no-borders-style} (if (nil? sprite) (sprites/EmptyComponent) sprite)])])]
         ]))

(defn create-sprites
  ([n m sprite-creator]
   (into [] (for [x (range 0 n)] (into [] (for [y (range 0 m)] (sprite-creator x y))))))
  ([n m] (create-sprites n m (fn [x y] nil))))

(defn merge-sprites [sprites upper-sprites offset-x offset-y]
  (create-sprites
    (count sprites) (count (sprites 0))
    (fn [x y]
      (let [original ((sprites x) y)
            ux (- x offset-x)
            uy (- y offset-y)]
        (if (and (>= ux 0) (< ux (count upper-sprites)))
          (if (and (>= uy 0) (< uy (count (upper-sprites 0))))
            ((upper-sprites ux) uy)
            original)
          original)))))

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

(def sprites-between-boards 4)
(def rockets-space-sprites-height 6)
(def board-sprites-height (inc state/size-n))
(def board-sprites-width (inc state/size-m))
(def boards-sprites-width (+ sprites-between-boards (* 2 board-sprites-width)))

(defn board-sprites [board]
  (let [selected (board :selected)
        shuffle-selected? (and (= (selected :x) 0) (= (selected :y) -1))]
    (-> (create-sprites board-sprites-height board-sprites-width)
        (merge-sprites (field-sprites (:cells board) selected) 0 1)
        (merge-sprites [[(sprites/ShuffleComponent (assoc board :selected? shuffle-selected?))]] (dec state/size-n) 0)
        (merge-sprites (create-sprites 1 state/size-m (fn [x y] (sprites/FireComponent))) state/size-n 1))))

(defn boards-sprites [boards]
  (-> (create-sprites board-sprites-height boards-sprites-width)
      (merge-sprites (board-sprites (:board1 boards)) 0 0)
      (merge-sprites (board-sprites (:board2 boards)) 0 (+ sprites-between-boards (inc state/size-m)))))

(def space-between-boards (* sprites/sprite-width sprites-between-boards))
(def board-width (* sprites/sprite-width (inc state/size-m)))
(def boards-width (+ board-width board-width space-between-boards))
(def board-height (* sprites/sprite-width (inc state/size-n)))
(def rockets-space-height (* sprites/sprite-width rockets-space-sprites-height))

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
    [:h1 {:style (assoc sprites/names-style :text-align "center")} name]))

(q/defcomponent
  PlayersComponent [args]
  (html
    [:div {:style {:position "absolute", :width boards-width, :top (:top args)}}
     [:div {:style {:position "absolute", :width (- board-width sprites/sprite-width), :left sprites/sprite-width}}
      (PlayerNameComponent (:player1 args))]
     [:div {:style {:position "absolute", :width (- board-width sprites/sprite-width), :left (+ board-width space-between-boards sprites/sprite-width)}}
      (PlayerNameComponent (:player2 args))]]))

(defn gamezone-sprites [data]
  (-> (create-sprites (+ rockets-space-sprites-height board-sprites-height) boards-sprites-width)
      (merge-sprites (boards-sprites data) rockets-space-sprites-height 0)))

(q/defcomponent
  GameComponent [data world-atom]
  (html
    [:div {:style {:width boards-width, :height (+ board-height rockets-space-height), :position "relative"}}
     [:div {:style {:position "absolute", :top 0}}
      [:table {:style util/no-borders-style}
       [:tr {:style util/no-borders-style}
        [:td {:style util/no-borders-style} (SpritesComponent (gamezone-sprites data))]]]]
     [:div {:style {:position "absolute"}} (RocketsComponent (:rockets data))]
     [:div {:style {:position "absolute"}} (PlayersComponent (assoc data :top (- rockets-space-height (* 5 sprites/sprite-width))))]
     [:div {:style {:position "absolute"}} (PlayersComponent {:player1 "W S A D + Q" :player2
                                                                       "Arrows + Space"
                                                              :top     (+ rockets-space-height board-height)})]
     ]))
