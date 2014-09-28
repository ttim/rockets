(ns rockets.model
  (:require
    [rockets.util :as util]
    [clojure.set :as cljset]))

(def size-n 8)
(def size-m size-n)
(def board-size (* size-n size-m))
;Note: sum of (prob-by-cell-types) must be equals to (board-size)
(def prob-by-cell-types [20 20 18 2 4])
(def rockets-cnt (divide size-m 2))
(def max-rocket-fuel 3)
(def max-time-to-reload 100)

(def wick-timeout 15)

(def rocket-max-progress 100)
(def rocket-state-staying :staying)
(def rocket-state-flying :flying)
(def rocket-state-dying :dying)

(defn pos [x y]
  {:x x
   :y y})

; 0 - top, 1 - right, 2 - down, 3 - left
(def cell-types
  [[#{} #{1 3} #{} #{1 3}]
   [#{0 1} #{0 1} #{} #{}]
   [#{0 1 3} #{0 1 3} #{} #{0 1 3}]
   [#{} #{1} #{} #{}]
   [#{0 1 2 3} #{0 1 2 3} #{0 1 2 3} #{0 1 2 3}]])

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

(defn generate-cell-type []
  (loop [i 0
         j (rand-int board-size)]
    (if (< j (prob-by-cell-types i))
      i
      (recur (inc i) (- j (prob-by-cell-types i))))))

(defn generate-board-cell []
  (cell (generate-cell-type) (rand-int 4) false))

(defn generate-board-cells []
  (into (vector) (for [_ (range 0 size-n)]
                   (into (vector) (for [_ (range 0 size-m)]
                                    (generate-board-cell))))))

(defn generate-board []
  {:selected       (pos 0 0)
   :cells          (generate-board-cells)
   :time-to-reload max-time-to-reload
   :wick-timers    (into (vector) (repeat size-m -1))})

(def dx [1 0 -1 0])
(def dy [0 1 0 -1])

(defn get-next-point [point direction]
  (pos (+ (point :x) (dx direction)) (+ (point :y) (dy direction))))

(defn reset-field? [point]
  (and (== 0 (point :x)) (== (point :y) -1)))

(defn board-point? [point]
  (and (<= 0 (point :x)) (< (point :x) size-n) (<= 0 (point :y)) (< (point :y) size-m)))

(defn valid-point? [point]
  (or (board-point? point)
      (reset-field? point)))

(defn do-move-selection [direction]
  (fn [board] (let [next-point (get-next-point (board :selected) direction)]
                (if (valid-point? next-point) (assoc-in board [:selected] next-point) board))))

(defn next-orientation [orientation]
  (mod (inc orientation) 4))

(defn do-rotate-cell [cell]
  (update-in cell [:orientation] next-orientation))

(defn do-rotate-selected [board]
  (update-in board [:cells ((board :selected) :x) ((board :selected) :y)] do-rotate-cell))

(defn next-cell [cr-pos direction]
  (pos (+ (cr-pos :x) (dx direction)) (+ (cr-pos :y) (dy direction))))

(defn revert-direction [direction]
  (mod (+ direction 2) 4))

(defn cell-have-conn? [board cell-pos conn-border]
  (if (or (< (cell-pos :y) 0) (<= size-m (cell-pos :y)))
    false
    (if (or (and (== (cell-pos :x) -1) (== conn-border 0)) (and (== (cell-pos :x) size-n) (== conn-border 2)))
      true
      (if-not (board-point? cell-pos)
        false
        (let [cell (((board :cells) (cell-pos :x)) (cell-pos :y))
              border-num (mod (- conn-border (cell :orientation)) 4)]
          (contains? ((cell-types (cell :cell-type)) border-num) border-num))))))

(defn next-cells [board cr-pos]
  (into
    (vector)
    (map (fn [i] (next-cell cr-pos i))
         (filter (fn [i] (and (cell-have-conn? board cr-pos i)
                              (cell-have-conn? board (next-cell cr-pos i) (revert-direction i))))
                 (range 0 4)))))

(defn connected-cells [board cr-pos]
  (let [visited (transient #{})]
    (do ((fn dfs [cr-pos]
           (do (conj! visited cr-pos)
               (doseq [next (next-cells board cr-pos)]
                 (if-not (contains? visited next)
                   (dfs next))
                 )))
         cr-pos)
        (persistent! visited))))

(defn do-reset-board [board]
  (if (== 0 (board :time-to-reload))
    (assoc board
      :cells (generate-board-cells)
      :time-to-reload max-time-to-reload
      :wick-timers (into (vector) (repeat size-m -1)))
    board))

(defn update-time-to-reload [time]
  (if (== time 0) 0 (dec time)))

(defn other-player [player]
  (if (= player :player1) :player2 :player1))

(defn live-rocket? [rocket]
  (not (and (== rocket-state-dying (rocket :state)) (<= rocket-max-progress (rocket :progress)))))

(defn next-rocket-state-flying [rocket]
  (if (<= rocket-max-progress (rocket :progress))
    (generate-rocket-staying (dec (rocket :fuel)) (other-player (rocket :source-player)) (rocket :target-slot))
    (update-in rocket [:progress] inc)))

(defn next-rocket-state-dying [rocket]
  (if (<= rocket-max-progress (rocket :progress))
    rocket
    (update-in rocket [:progress] inc)))

(defn update-rocket [rocket]
  (cond
    (== rocket-state-flying (rocket :state)) (next-rocket-state-flying rocket)
    (== rocket-state-dying (rocket :state)) (next-rocket-state-dying rocket)
    :else rocket))

(defn update-rockets [rockets]
  (into (vector) (filter live-rocket? (map update-rocket rockets))))

(def all-points
  (set
    (flatten
      (for [i (range 0 size-n)]
        (for [j (range 0 size-m)]
          (pos i j))))))

(defn do-color-cells
  ([board positions]
   (do-color-cells (do-color-cells board positions true)
                   (into (vector) (clojure.set/difference all-points (set (flatten positions))))
                   false))
  ([board positions val]
   (if (or (nil? positions) (empty? positions))
     board
     (let [cr-pos (positions 0)]
       (do-color-cells
         (assoc-in board [:cells (cr-pos :x) (cr-pos :y) :locked] val)
         (subvec positions 1)
         val)))))

(defn do-color-wicks-board [board]
  (do-color-cells board
                  (into
                    (vector)
                    (filter board-point?
                            (reduce cljset/union (for [i (range 0 size-m)] (connected-cells board (pos -1 i))))))))

(defn do-color-wicks [game-state board-num]
  (assoc-in game-state [board-num] (do-color-wicks-board (game-state board-num))))

(defn reset-wicks [board conn-list]
  (reduce (fn [board wick-num] (assoc-in board [:wick-timers wick-num] -1))
          (flatten [board (map (fn [cr-pos] (cr-pos :y)) (filter (fn [cr-pos] (== (cr-pos :x) -1)) conn-list))])))

(defn get-busy-slots [rockets player]
  (set (filter (fn [slot] (not= -1 slot))
               (map (fn [rocket]
                      (cond
                        (and (= (rocket :state) rocket-state-staying) (= (rocket :source-player) player)) (rocket :source-slot)
                        (and (= (rocket :state) rocket-state-flying) (not (= (rocket :source-player) player))) (rocket :target-slot)
                        :else -1)) rockets))))

(defn get-free-slots [rockets player]
  (clojure.set/difference (set (range 0 size-m)) (get-busy-slots rockets player)))

(defn run-rockets [game-state conn-list source-player]
  (assoc-in game-state [:rockets]
            (let [rockets (game-state :rockets)
                  fired (set (map (fn [cr-pos] (cr-pos :y)) (filter (fn [cr-pos] (== (cr-pos :x) size-n)) conn-list)))
                  free-slots (shuffle (into (vector) (get-free-slots rockets (other-player source-player))))]
              (if (empty? rockets)
                []
                (into
                  (vector)
                  (loop [rocket-num 0
                         slot-num 0
                         result []]
                    (let [rocket (rockets rocket-num)
                          is-rocket-takes-slot (and (= (rocket :source-player) source-player) (= (rocket :state) rocket-state-staying) (contains? fired (rocket :source-slot)))
                          is-rocket-died (== 0 (rocket :fuel))
                          new-rocket (if is-rocket-takes-slot
                                       (if is-rocket-died
                                         (generate-rocket-dying 0 (rocket :source-player) (rocket :source-slot))
                                         (generate-rocket-flying 0 (rocket :fuel) (rocket :source-player) (rocket :source-slot) (free-slots slot-num)))
                                       rocket)]
                      (if (< (inc rocket-num) (count rockets))
                        (recur (inc rocket-num) (if (and is-rocket-takes-slot (not is-rocket-died)) (inc slot-num) slot-num) (conj result new-rocket))
                        (conj result new-rocket)))))))))

(defn clean-board [game-state board-num conn-set]
  (assoc-in game-state
            [board-num :cells]
            (let [cells ((game-state board-num) :cells)]
              (loop [j 0
                     i 0
                     k 0
                     cells cells]
                (cond
                  (= j size-m) cells
                  (= i size-n) (recur (inc j) 0 0 cells)
                  (= k size-n) (recur j (inc i) k (assoc-in cells [i j] (generate-board-cell)))
                  (contains? conn-set (pos k j)) (recur j i (inc k) cells)
                  :else (recur j (inc i) (inc k) (assoc-in cells [i j] ((cells k) j))))))))

(defn rocket-exist [rockets player conn-set]
  (if (empty? rockets)
    false
    (let [rocket (rockets 0)]
      (or
        (and (= (rocket :state) rocket-state-staying)
             (= (rocket :source-player) player)
             (contains? conn-set (pos size-n (rocket :source-slot))))
        (rocket-exist (subvec rockets 1) player conn-set)))))

(defn update-wick [game-state board-num wick-num conn-list]
  (let [player (if (= board-num :board1) :player1 :player2)]
    (if-not (rocket-exist (game-state :rockets) player conn-list)
      (assoc-in game-state [board-num :wick-timers wick-num] -1)
      (if (== (((game-state board-num) :wick-timers) wick-num) 0)
        (clean-board
          (run-rockets
            (assoc-in game-state [board-num] (reset-wicks (game-state board-num) conn-list))
            conn-list
            player)
          board-num conn-list)
        (if (== (((game-state board-num) :wick-timers) wick-num) -1)
          (assoc-in game-state [board-num :wick-timers wick-num] wick-timeout)
          (update-in game-state [board-num :wick-timers wick-num] dec))))))

(defn do-fire-wicks [game-state board-num]
  (let [conn-cells (into (vector) (for [i (range 0 size-m)] (connected-cells (game-state board-num) (pos -1 i))))]
    (reduce (fn [game-state wick-num] (update-wick game-state board-num wick-num (conn-cells wick-num)))
            (flatten [game-state (range 0 size-m)]))))

(defn do-win [game-state winner]
  (-> game-state
      (assoc :type :finish)
      (assoc :winner winner)))

(defn check-win [game-state player]
  (if (and (= (:type game-state) :game) (empty? (get-busy-slots (game-state :rockets) player)))
    (do-win game-state player)
    game-state))

(defn generate-game-state [player1 player2]
  {:type    :game
   :player1 player1
   :player2 player2
   :board1  (do-color-wicks-board (generate-board))
   :board2  (do-color-wicks-board (generate-board))
   :rockets (generate-rockets)})

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
  (do-win game-state :player1))

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
  (if (not (= (:type game-state) :start))
    (-> game-state
        (check-win :player1)
        (check-win :player2)
        ; update ticks in case of finished game too: we need flying rockets at background =)
        (update-in [:board1 :time-to-reload] update-time-to-reload)
        (update-in [:board2 :time-to-reload] update-time-to-reload)
        (do-fire-wicks :board1)
        (do-fire-wicks :board2)
        (do-color-wicks :board1)
        (do-color-wicks :board2)
        (update-in [:rockets] update-rockets))
    game-state))
