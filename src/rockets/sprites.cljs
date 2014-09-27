(ns rockets.sprites
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]))

(def sprite-width 48)

(def base-style (merge {:width sprite-width, :height sprite-width, :background-image "url(../img/dummy.png)"} util/no-borders-style))

(defn select-type [style type]
  (if (= type -1)
    base-style
    (assoc style :background-image (str "url(../img/cell" type ".png)"))))

(defn rotate [style angle]
  (assoc style :transform (str "rotate(" (* angle 90) "deg)"))
  )

(defn sprite [type angle]
  [:div {:style (rotate (select-type base-style type) angle)}]
  )

(q/defcomponent
  SpriteComponent []
  (html (sprite -1 0)))

(q/defcomponent
  CoolSpriteComponent [type angle]
  (html (sprite type angle)))