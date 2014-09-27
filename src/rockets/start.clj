(ns rockets.start
  (:require
    [figwheel.client :as fw]
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.model_sample :as sample]
    [clojure.browser.dom :as cljsdom]))

(q/defcomponent
  StartComponent [data]
  (html
    [:div
     [:h1 "Welcome, Awesome Rocketeers!"]
     "Player 1"
     [:input {:type "text", :value (:player1 data), :on-change #(update-text :player1 (-> % .-target .-value))}]
     [:p]
     "Player 2"
     [:input {:type "text", :value (:player2 data), :on-change #(update-text :player2 (-> % .-target .-value))}]
     [:p]
     [:button
      {:type     "button"
       :disabled (or (string/blank? (:player1 data)) (string/blank? (:player2 data)))
       }
      "Go!"]
     ]))

; define render function
#_(defn render-start-screen [data]
  (q/render
    (StartComponent data)
    (.getElementById js/document "main-area")))
