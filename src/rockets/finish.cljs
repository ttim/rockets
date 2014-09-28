(ns rockets.finish
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]
    [rockets.game :as game]
    [rockets.model :as model]))

(q/defcomponent
  FinishComponent [data world-atom]
  (html
    [:div {:style {:width game/boards-width :position "absolute" :height (+ game/board-height game/rockets-space-height)}}
     [:div.dim.elonmusk {:style {:position "absolute"}} (game/GameComponent data world-atom)]
     [:div.dialogWrapper {:style {:position "absolute" :text-align "center"}}
      [:div.dialog
       [:div.titleText (str ((:winner data) data) " Won!")]
       [:p]
       [:button.button
        {:type     "button"
         :on-click #(reset! world-atom (model/generate-game-state (:player1 data) (:player2 data) (:audio? data)))
         }
        "Play again"
        ]
       [:p]
       [:div.normalText "If You Like Our Game"]
       [:button.voteButton {:on-click #(util/redirect-to util/clojurecup-app-url)} "Vote For Us!"]
       ]
      ]
     ]
    ))
