(ns rockets.sprites
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]))

(def sprite-width 36)
(def rocket-style (merge {:width sprite-width :height (* 2 sprite-width) :background-image "url(../img/rocket.png)"} util/no-borders-style))
(def rocket-fire-style (merge {:width sprite-width :height (* 2 sprite-width) :background-image "url(../img/rocket-fire.png)"} util/no-borders-style))

(def base-style (merge {:width sprite-width :height sprite-width} util/no-borders-style))

(def cell-style (merge base-style {:background-image "url(../img/dummy.png)"} util/no-borders-style))
(def empty-style (merge base-style {:background-image "url(../img/empty.png)"} util/no-borders-style))
(def shuffle-style (merge base-style {:background-image "url(../img/shuffle.png)"} util/no-borders-style))
(def fire-style (merge base-style {:background-image "url(../img/fire.png)"} util/no-borders-style))

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

(defn fire-state [sprite fire?]
  (if fire? [:div {:style {:background-color "#ffaa22"}} sprite] sprite))

(defn selected-state [sprite selected?]
  (if selected? [:div {:style {:background-color "#997777"}} sprite] sprite))

(q/defcomponent
  ShuffleComponent [refresh-time]
  (html [:div {:style shuffle-style}]))

(q/defcomponent
  RocketComponent [fire?]
  (html [:div {:style (if fire? rocket-fire-style rocket-style)}]))

(q/defcomponent
  EmptyComponent []
  (html [:div {:style empty-style}]))

(q/defcomponent
  FireComponent []
  (html [:div {:style fire-style}]))

(q/defcomponent
  ;CoolSpriteComponent [type, angle, fire?, selected?]
  CoolSpriteComponent [args]
  (html (selected-state (fire-state(sprite (args :type) (args :angle)) (args :fire?)) (args :selected?))))