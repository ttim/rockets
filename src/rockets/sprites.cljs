(ns rockets.sprites
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]))

(def sprite-width 48)

(def rocket-style (merge {:width sprite-width :height (* 3 sprite-width) :background-image "url(../img/rocket.png)"} util/no-borders-style))

(def base-style (merge {:width sprite-width :height sprite-width} util/no-borders-style))

(def cell-style (merge base-style {:background-image "url(../img/dummy.png)"} util/no-borders-style))
(def empty-style (merge base-style {:background-image "url(../img/empty.png)"} util/no-borders-style))

(defn select-type [style type]
  (if (= type -1)
    style
    (assoc style :background-image (str "url(../img/cell" type ".png)"))))

(defn rotate [style angle]
  (assoc style :transform (str "rotate(" (* angle 90) "deg)"))
  )

(defn sprite [type angle]
  [:div {:style (rotate (select-type cell-style type) angle)}]
  )

(q/defcomponent
  SpriteComponent []
  (html (sprite -1 0)))

(q/defcomponent
  RocketComponent []
  (html [:div {:style rocket-style}]))

(q/defcomponent
  EmptyComponent []
  (html [:div {:style empty-style}]))

(q/defcomponent
  CoolSpriteComponent [type angle]
  (html (sprite type angle)))