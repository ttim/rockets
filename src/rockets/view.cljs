(ns rockets.view
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]
    [rockets.sprites :as sprites :refer [create-sprites, add-table, add-sprites, sprite-width, add-zone, single-sprite]]
    [rockets.state :as state]))

; start
(q/defcomponent
  StartComponent [data world-atom]
  (html
    [:div.usual-background {:style {:text-align "center" :position "relative" :top "50%" :transform "translateY(50%)"}}
     [:div.titleText {:style {:text-align "center"}} "Welcome, Rocketeers!"]
     [:p]
     [:input.inputField {:type "text", :value (:player1 data), :on-change #(util/update-text world-atom :player1 (-> % .-target .-value))}]
     [:p]
     [:input.inputField {:type "text", :value (:player2 data), :on-change #(util/update-text world-atom :player2 (-> % .-target .-value))}]
     [:p]
     [:button.button
      {:type     "button"
       :disabled (or (string/blank? (:player1 data)) (string/blank? (:player2 data)))
       :on-click #(swap! world-atom state/start-game)
       }
      "Go!"]]))

; game
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
  (-> (create-sprites state/size-n state/size-m)
      (add-table #(let [x (- state/size-n (inc %1))
                        y %2
                        cell ((cells x) y)]
                   (sprites/CoolSpriteComponent {:type      (:cell-type cell),
                                                 :angle     (:orientation cell),
                                                 :selected? (and (= x (:x selected)) (= y (:y selected))),
                                                 :fire?     (:locked cell)})))))

(defn fire-sprites []
  (-> (create-sprites 1 state/size-m)
      (add-table (fn [i j] (sprites/FireComponent)))))

(defn board-sprites [board]
  (let [selected (board :selected)
        shuffle-selected? (and (= (selected :x) 0) (= (selected :y) -1))]
    (-> (create-sprites board-sprites-height board-sprites-width)
        (add-sprites 0 1 (field-sprites (:cells board) selected))
        (add-sprites (dec state/size-n) 0 (single-sprite (sprites/ShuffleComponent (assoc board :selected? shuffle-selected?))))
        (add-sprites state/size-n 1 (fire-sprites)))))

(defn boards-sprites [boards]
  (-> (create-sprites board-sprites-height boards-sprites-width)
      (add-sprites 0 0 (board-sprites (:board1 boards)))
      (add-sprites 0 (+ sprites-between-boards (inc state/size-m)) (board-sprites (:board2 boards)))))

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
  (let [keys-offset-top (+ rockets-space-sprites-height board-sprites-height)
        players-name-offset-top (- rockets-space-sprites-height player-name-sprite-offset-from-board)
        player1-offset-left 1
        player2-offset-left (+ 1 board-sprites-width sprites-between-boards)]
    (-> (create-sprites gamezone-height boards-sprites-width)
        (add-sprites rockets-space-sprites-height 0 (boards-sprites data))
        (add-zone keys-offset-top player1-offset-left 2 state/size-n (HeaderComponent "W S A D + Q"))
        (add-zone keys-offset-top player2-offset-left 2 state/size-n (HeaderComponent "Arrows + Space"))
        (add-zone players-name-offset-top player1-offset-left 2 state/size-n (HeaderComponent (:player1 data)))
        (add-zone players-name-offset-top player2-offset-left 2 state/size-n (HeaderComponent (:player2 data)))
        (add-zone 0 0 rockets-space-sprites-height boards-sprites-width (RocketsComponent (:rockets data))))))

(q/defcomponent GameComponent [data world-atom]
  (sprites/SpritesComponent ((util/with-time-debug :build-sprites #(gamezone-sprites data)))))

; finish
(q/defcomponent
  FinishComponent [data world-atom]
  (html
    [:div {:style {:width (* (inc boards-sprites-width) sprite-width) :position "absolute" :height (* gamezone-height sprite-width)}}
     [:div.dim {:style {:position "absolute"}} (GameComponent data world-atom)]
     [:div.dialogWrapper {:style {:position "absolute" :text-align "center"}}
      [:div.dialog
       [:img {:src "img/elonmusk.jpg"}]
       [:div.titleText (str ((:winner data) data) " Won!")]
       [:p]
       [:button.button
        {:type     "button"
         :on-click #(swap! world-atom state/start-game)
         }
        "Play again"
        ]]]]))

; whole world
(q/defcomponent
  WorldComponent [data world-atom]
  (html
    [:div
     [:div {:style {:width (* (inc boards-sprites-width) sprite-width), :margin "0 auto"}}
      (case (:type data)
        :start (StartComponent data world-atom)
        :game (GameComponent data world-atom)
        :finish (FinishComponent data world-atom))]
     [:div {:id "footer"}
      [:div.titleText {:id "left-footer"}
       [:div {:style {:float "left" :margin-right 5}} "Awesome Rocketeers"]
       [:a {:href "https://github.com/ttim/rockets"} [:div.github]]]
      [:div.titleText {:id "right-footer"}
       [:button.button {:on-click #(util/update-text world-atom :audio? (not (:audio? data)))
                        :style    {:height 32 :width 32 :margin-right 5 :background-image (str "url(img/" (if (:audio? data) "sound_on" "sound_off") ".png)")}} "."]
       [:button.button {:on-click #(util/redirect-to "about.html")
                        :style {:margin-right 5}} "About"]
       [:button.button {:on-click #(util/redirect-to util/twitter-share-url)} "Share"]]]]))
