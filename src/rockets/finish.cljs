(ns rockets.finish
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]
    [rockets.game :as game]))

(q/defcomponent
  FinishComponent [data world-atom]
  (html
    [:div {:style {:width game/boards-width :position "absolute" :height (+ game/board-height game/rockets-space-height)}}
     [:div.dim {:style {:position "absolute"}} (game/GameComponent data world-atom)]
     [:div.dialogWrapper {:style {:position "absolute"}}
      [:div.dialog.titleText (str ((:winner data) data) " Won!")
       [:p]
       [:button.button "Play again"]
       ]
      ]
     ]
    ))
