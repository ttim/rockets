(ns rockets.model_sample)

(def start-state
  {:type    :start
   :player1 "First Rocketeer"
   :player2 "Second Rocketeer"})

(defn generate-board
  [] [])

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

