(ns rockets.start
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]
    [rockets.model :as model]))

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
       :on-click #(reset! world-atom (model/generate-game-state (:player1 data) (:player2 data)))
       }
      "Go!"]
     ]))
