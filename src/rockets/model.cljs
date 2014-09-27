(ns rockets.model
  (:require
    [rockets.util :as util]))

(def start-state
  {:type    :start
   :player1 "First Rocketeer"
   :player2 "Second Rocketeer"})

(def size-n 8)
(def size-m 8)
(def rockets-cnt 4)
(def max-rocket-fuel 3)
(def max-reload-time 100)

(def rocket-state-staying :staying)
(def rocket-state-flying :flying)
(def rocket-state-dying :dying)

(defn pos [x y]
  {:x x
   :y y})

; 0 - top, 1 - right, 2 - down, 3 - left
(def cell-types
  [[[] [3] [] [1]]
   [[1] [0] [] []]
   [[1 3] [0 3] [] [0 1]]
   [[] [] [] []]
   [[1 2 3] [0 2 3] [0 1 3] [0 1 2]]])

(defn cell [cell-type orientation locked]
  {:cell-type   cell-type
   :orientation orientation
   :locked      locked
   })

(defn generate-rocket [state progress source-player source-slot]
  {:state         state
   :progress      progress
   :fuel          max-rocket-fuel
   :source-player source-player
   :source-slot   source-slot})

(defn generate-rockets
  ([] (into (vector) (concat (generate-rockets :player1) (generate-rockets :player2))))
  ([source-player]
   (let [slots (shuffle (range 0 size-m))]
     (into (vector) (for [i (range 0 rockets-cnt)]
                      (generate-rocket rocket-state-staying 0 source-player (slots i)))))))

(defn generate-board-cell []
  (cell (rand-int (clojure.core/count cell-types)) (rand-int 4) false))

(defn generate-board-cells []
  (into (vector) (for [i (range 0 size-n)]
                   (into (vector) (for [j (range 0 size-m)]
                                    (generate-board-cell))))))

(defn generate-board []
  {:selected    (pos 0 0)
   :cells       (generate-board-cells)
   :reload-time max-reload-time})

(def game-state
  (let [board1 (generate-board)
        board2 (generate-board)]
    {:type    :game
     :player1 "name1"
     :player2 "name2"
     :board1  board1
     :board2  board2
     :rockets (generate-rockets)}))

(def finish-state
  {:type    :finish
   :player1 "name1"
   :player2 "name2"
   :win     :player1})


(defn event-move-selection [game-state board direction]
  game-state)

(defn next-orientation [orientation]
  (rem (inc orientation) 4))

(defn do-rotate-selected [board]
  (util/update-value board [:cells ((board :selected) :x) ((board :selected) :y) :orientation] next-orientation))

(defn event-select [game-state board]
  ;todo add action when selected in reset field
  (util/update-value game-state [board] do-rotate-selected))

(defn event-tick [game-state]
  game-state)
