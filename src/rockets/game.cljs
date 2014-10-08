(ns rockets.game
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]
    [rockets.sprites :as sprites]
    [rockets.state :as state]))

; start sprites framework
(defn create-sprites
  ([n m sprite-creator]
   (into [] (for [x (range 0 n)] (into [] (for [y (range 0 m)] (sprite-creator x y))))))
  ([n m] (create-sprites n m (fn [x y] nil))))

(defn sh [sprites] (count sprites))
(defn sw [sprites] (count (sprites 0)))

(defn merge-sprites [sprites upper-sprites offset-x offset-y]
  (create-sprites
    (sh sprites) (sw sprites)
    (fn [x y]
      (let [original ((sprites x) y)
            ux (- x offset-x)
            uy (- y offset-y)]
        (if (and (>= ux 0) (< ux (sh upper-sprites)))
          (if (and (>= uy 0) (< uy (sw upper-sprites)))
            ((upper-sprites ux) uy)
            original)
          original)))))

; args {:sprites, :zones}
(q/defcomponent
  SpritesComponent [args]
  (let [{:keys [sprites zones]} args]
    (html [:div {:style {
                          :border   "1px double white"
                          :position "relative"
                          :width    (* (sw sprites) sprites/sprite-width)
                          :height   (* (sh sprites) sprites/sprite-width)}}
           (for [zone zones
                 :let [{:keys [offset-x, offset-y, width, height, component]} zone]]
             [:div {:style {
                             :position "absolute"
                             :width    (* width sprites/sprite-width)
                             :height   (* height sprites/sprite-width)
                             :top      (* offset-x sprites/sprite-width)
                             :left     (* offset-y sprites/sprite-width)}} component])

           [:table {:style (merge util/no-borders-style {:position "absolute"})}
            (for [sprites-line sprites]
              [:tr {:style util/no-borders-style}
               (for [sprite sprites-line]
                 [:td {:style util/no-borders-style} (if (nil? sprite) (sprites/EmptyComponent) sprite)])])]
           ])))
; end sprites framework

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
(def keys-zone-sprites-height 2)
(def player-name-sprite-offset-from-board 5)

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
  HeaderComponent [name]
  (html
    [:h1 {:style (assoc sprites/names-style :text-align "center")} name]))

(defn gamezone-sprites [data]
  (-> (create-sprites (+ rockets-space-sprites-height board-sprites-height keys-zone-sprites-height) boards-sprites-width)
      (merge-sprites (boards-sprites data) rockets-space-sprites-height 0)))

(q/defcomponent
  EmptyWhiteComponent []
  (html [:div {:style {:background-color "white" :width "100%" :height "100%"}}]))

(q/defcomponent
  GameComponent [data world-atom]
  (let [keys-offset-x (+ rockets-space-sprites-height board-sprites-height)
        players-name-offset-x (- rockets-space-sprites-height player-name-sprite-offset-from-board)
        player1-offset-y 1
        player2-offset-y (+ 1 board-sprites-width sprites-between-boards)]
    (SpritesComponent
      {:sprites (gamezone-sprites data),
       :zones   [
                  {:offset-x  keys-offset-x, :offset-y player1-offset-y
                   :width     state/size-n, :height 2
                   :component (HeaderComponent "W S A D + Q")}
                  {:offset-x  keys-offset-x :offset-y player2-offset-y
                   :width     state/size-n :height 2
                   :component (HeaderComponent "Arrows + Space")}

                  {:offset-x  players-name-offset-x :offset-y player1-offset-y
                   :width     state/size-n :height 2
                   :component (HeaderComponent (:player1 data))}
                  {
                    :offset-x  players-name-offset-x :offset-y player2-offset-y
                    :width     state/size-n :height 2
                    :component (HeaderComponent (:player2 data))}

                  {
                    :offset-x  0 :offset-y 0
                    :width     boards-sprites-width :height rockets-space-sprites-height
                    :component (RocketsComponent (:rockets data))}
                  ]})))
