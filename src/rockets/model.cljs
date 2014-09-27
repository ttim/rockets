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
  {:selected    (pos 0 0)
   :cells       (generate-board-cells)
   :reload-time max-reload-time})

(defn generate-game-state [player1 player2]
  {:type    :game
   :player1 player1
   :player2 player2
   :board1  (generate-board)
   :board2  (generate-board)
   :rockets (generate-rockets)})

(def game-state
  (let [state (generate-game-state "name1" "name2")]
    (let [state (util/set-value state [:rockets 0] (assoc ((state :rockets) 0) :state rocket-state-flying :progress 30 :target-slot 0))]
      (let [state (util/set-value state [:rockets 1] (assoc ((state :rockets) 1) :state rocket-state-dying :progress 60))]
        (let [state (util/set-value state [:rockets 2] (assoc ((state :rockets) 2) :state rocket-state-dying :progress 99))]
          state)))))

(def finish-state
  {:type    :finish
   :player1 "name1"
   :player2 "name2"
   :win     :player1})

(def dx [1 0 -1 0])
(def dy [0 1 0 -1])

(defn get-next-point [point direction]
  (pos (+ (point :x) (dx direction)) (+ (point :y) (dy direction))))

(defn valid-point? [point]
  (or (and (<= 0 (point :x)) (< (point :x) size-n) (<= 0 (point :y)) (< (point :y) size-m))
      (and (== 0 (point :x)) (== (point :y) -1))))

(defn do-move-selection [direction]
  (fn [board] (let [next-point (get-next-point (board :selected) direction)]
                (if (valid-point? next-point) (util/set-value board [:selected] next-point) board))))

(defn next-orientation [orientation]
  (mod (inc orientation) 4))

(defn do-rotate-cell [cell]
  (if (cell :locked) cell (util/update-value cell [:orientation] next-orientation)))

(defn do-rotate-selected [board]
  (util/update-value board [:cells ((board :selected) :x) ((board :selected) :y)] do-rotate-cell))


;====================================;
;                                    ;
;         Public API: events         ;
;                                    ;
;====================================;

(defn event-move-selection [game-state board direction]
  (util/update-value game-state [board] (do-move-selection direction)))

(defn event-select [game-state board]
  ;(js/console.log (sablono.util/to-str ((:cells (game-state board)) 0)))
  ;todo add action when selected in reset field
  (util/update-value game-state [board] do-rotate-selected))

(defn event-tick [game-state tick]
  (if (= (mod tick 10) 0) (event-select game-state :board1) game-state))
