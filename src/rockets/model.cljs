(ns rockets.model)

(def start-state
  {:type    :start
   :player1 "First Rocketeer"
   :player2 "Second Rocketeer"})

(def size-n 8)
(def size-m 8)
(def rockets-cnt 4)

(defn pos [x y]
  {:x x
   :y y})

; 0 - top, 1 - right, 2 - down, 3 - left
(def cell-types
  [[[2] [] [0] []]
   [[1 3] [0 3] [] [0 1]]
   [[1] [0] [] []]
   [[1 2 3] [0 2 3] [0 1 3] [0 1 2]]
   [[] [3] [] [1]]
   [[] [] [] []]])

(defn cell [cell-type orientation locked]
  {:cell-type   cell-type
   :orientation orientation
   :locked      locked
   })

(defn generate-rocket [state progress]
  {:state    state
   :progress progress})

(defn generate-field []
  [])

(defn generate-board-cell []
  (cell (rand-int (clojure.core/count cell-types)) (rand-int 4) false))

(defn generate-cells []
  (into (vector) (for [i (range 0 size-n)]
                   (into (vector) (for [j (range 0 size-m)]
                                    (generate-board-cell))))))

(defn generate-board []
  {:selected (pos 0 0)
   :cells    (generate-cells)})

(defn generate-rockets
  [b1 b2] [])

(def game-state
  (let [board1 (generate-board)
        board2 (generate-board)]
    {:type    :game
     :player1 "name1"
     :player2 "name2"
     :board1  board1
     :board2  board2
     :rockets (generate-rockets board1 board2)}))

(def finish-state
  {:type    :finish
   :player1 "name1"
   :player2 "name2"
   :win     :player1})

