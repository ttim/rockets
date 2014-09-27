(ns rockets.core
  (:require
    [figwheel.client :as fw]
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.model_sample :as sample]))

; world state
(defonce world (atom sample/start-state))

(defn update-text
  [key value] (reset! world (assoc @world key value)))

; define component
(q/defcomponent
  Root [data]
  (html
    [:div
     [:h1 "Welcome, Awesome Rocketeers!"]
     "Player 1"
     [:input {:type "text", :value (:player1 data), :on-change #(update-text :player1 (-> % .-target .-value))}]
     [:p]
     "Player 2"
     [:input {:type "text", :value (:player2 data), :on-change #(update-text :player2 (-> % .-target .-value))}]
     [:h1 (str "First player name is " (:player1 data))]
     [:h1 (str "Second player name is " (:player2 data))]
     [:p]
     [:button
      {:type "button"
       :disabled (or (string/blank? (:player1 data)) (string/blank? (:player2 data)))
       }
      "Go!"]
     ]))

; define render function
(defn render [data]
  (q/render
    (Root data)
    (.getElementById js/document "main-area")))

; render for first time
(defonce _first_time_render (render @world))

; watch world state and re-render in case of change
(add-watch
  world ::render
  (fn [_ _ _ data] (render data)))

; watch js code reload, by this code we are forcing world atom update without update itself
(fw/watch-and-reload
  :jsload-callback
  (fn [] (swap! world update-in [:tmp-dev] not)))
