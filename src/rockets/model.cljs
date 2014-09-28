(ns rockets.model
  (:require
    [rockets.util :as util]))

(def size-n 8)
(def size-m 8)
(def rockets-cnt 4)
(def max-rocket-fuel 3)
(def max-time-to-reload 100)

(def rocket-max-progress 100)
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

(defn generate-rocket-staying [fuel source-player source-slot]
  {:state         rocket-state-staying
   :fuel          fuel
   :source-player source-player
   :source-slot   source-slot})

(defn generate-rocket-flying [progress fuel source-player source-slot target-slot]
  {:state         rocket-state-flying
   :progress      progress
   :fuel          fuel
   :source-player source-player
   :source-slot   source-slot
   :target-slot   target-slot})

(defn generate-rocket-dying [progress source-player source-slot]
  {:state         rocket-state-dying
   :progress      progress
   :source-player source-player
   :source-slot   source-slot})

(defn generate-rockets
  ([] (into (vector) (concat (generate-rockets :player1) (generate-rockets :player2))))
  ([source-player]
   (let [slots (shuffle (range 0 size-m))]
     (into (vector) (for [i (range 0 rockets-cnt)]
                      (generate-rocket-staying max-rocket-fuel source-player (slots i)))))))

(defn generate-board-cell []
  (cell (rand-int (clojure.core/count cell-types)) (rand-int 4) false))

(defn generate-board-cells []
  (into (vector) (for [_ (range 0 size-n)]
                   (into (vector) (for [_ (range 0 size-m)]
                                    (generate-board-cell))))))

(defn generate-board []
  {:selected       (pos 0 0)
   :cells          (generate-board-cells)
   :time-to-reload max-time-to-reload})

(defn generate-game-state [player1 player2]
  {:type    :game
   :player1 player1
   :player2 player2
   :board1  (generate-board)
   :board2  (generate-board)
   :rockets (generate-rockets)})

(def dx [1 0 -1 0])
(def dy [0 1 0 -1])

(defn get-next-point [point direction]
  (pos (+ (point :x) (dx direction)) (+ (point :y) (dy direction))))

(defn reset-field? [point]
  (and (== 0 (point :x)) (== (point :y) -1)))

(defn valid-point? [point]
  (or (and (<= 0 (point :x)) (< (point :x) size-n) (<= 0 (point :y)) (< (point :y) size-m))
      (reset-field? point)))

(defn do-move-selection [direction]
  (fn [board] (let [next-point (get-next-point (board :selected) direction)]
                (if (valid-point? next-point) (assoc-in board [:selected] next-point) board))))

(defn next-orientation [orientation]
  (mod (inc orientation) 4))

(defn do-rotate-cell [cell]
  (if (cell :locked) cell (update-in cell [:orientation] next-orientation)))

(defn do-rotate-selected [board]
  (update-in board [:cells ((board :selected) :x) ((board :selected) :y)] do-rotate-cell))

(defn do-reset-board [board]
  (if (== 0 (board :time-to-reload))
    (assoc board :cells (generate-board-cells) :time-to-reload max-time-to-reload)
    board))

(defn update-time-to-reload [time]
  (if (== time 0) 0 (dec time)))

(defn other-player [player]
  (if (= player :player1) :player2 :player1))

(defn live-rocket? [rocket]
  (not (and (== rocket-state-dying (rocket :state)) (== rocket-max-progress (rocket :progress)))))

(defn next-rocket-state-flying [rocket]
  (if (== rocket-max-progress (rocket :progress))
    (generate-rocket-staying (dec (rocket :fuel)) (other-player (rocket :source-player)) (rocket :target-slot))
    (update-in rocket [:progress] inc)))

(defn next-rocket-state-dying [rocket]
  (if (== rocket-max-progress (rocket :progress))
    rocket
    (update-in rocket [:progress] inc)))

(defn update-rocket [rocket]
  (cond
    (== rocket-state-flying (rocket :state)) (next-rocket-state-flying rocket)
    (== rocket-state-dying (rocket :state)) (next-rocket-state-dying rocket)
    :else rocket))

(defn update-rockets [rockets]
  (filter live-rocket? (map update-rocket rockets)))

; states samples
(def start-state
  {:type    :start
   :player1 "First Rocketeer"
   :player2 "Second Rocketeer"})

(def game-state
  (-> (generate-game-state "name1" "name2")
      (update-in [:rockets 0] #(assoc % :state rocket-state-flying :progress 0 :target-slot 0))
      (update-in [:rockets 1] #(assoc % :state rocket-state-dying :progress 60))
      (update-in [:rockets 2] #(assoc % :state rocket-state-dying :progress 0))))

(def finish-state
  {:type    :finish
   :player1 "First Rocketeer"
   :player2 "Second Rocketeer"
   :win     :player1})

; states modifiers
(defn move [game-state board direction]
  (case (:type game-state)
    :game (update-in game-state [board] (do-move-selection direction))
    game-state))

(defn rotate [game-state board]
  (case (:type game-state)
    :game (update-in game-state [board] (if (reset-field? ((game-state board) :selected)) do-reset-board do-rotate-selected))
    game-state))

(defn tick [game-state tick-value]
  (case (:type game-state)
    :game (-> game-state
              (update-in [:board1 :time-to-reload] update-time-to-reload)
              (update-in [:board2 :time-to-reload] update-time-to-reload)
              (update-in [:rockets] update-rockets))
    game-state))
