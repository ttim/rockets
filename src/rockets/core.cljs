(ns rockets.core
  (:require
    [figwheel.client :as fw]
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]))

; world state
(defonce world (atom {:text "Hello!"}))

; define component
(q/defcomponent
  Root [data]
  (html
    [:h1 (:text data)]))

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
