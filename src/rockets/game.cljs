(ns rockets.game
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]))

(def sprite-width "48px")

(q/defcomponent
  Sprite []
  (html
    [:div {:style {:width sprite-width, :height sprite-width, :background-color "#ffffff"}}]))

(q/defcomponent
  GameComponent [data world-atom]
  (Sprite))
