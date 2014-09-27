(ns rockets.sprites
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]))

(def sprite-width "48px")

(def sprite
  [:div {:style {:width sprite-width, :height sprite-width, :background-image "url(../img/dummy.png)"}}]
  )

(q/defcomponent
  SpriteComponent []
  (html sprite))