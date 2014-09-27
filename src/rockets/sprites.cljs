(ns rockets.sprites
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]))

(def sprite-width 36)
(def rocket-style (merge {:width sprite-width :height (* 2 sprite-width) :background-image "url(../img/generated/rocket.png)"} util/no-borders-style))
(def rocket-fire-style (merge {:width sprite-width :height (* 2 sprite-width) :background-image "url(../img/generated/rocket-fire.png)"} util/no-borders-style))

(def base-style (merge {:width sprite-width :height sprite-width} util/no-borders-style))

(def empty-style (merge base-style {:background-image "url(../img/empty.png)"} util/no-borders-style))
(def shuffle-style (merge base-style {:background-image "url(../img/generated/shuffle.png)"} util/no-borders-style))
(def fire-style (merge base-style {:background-image "url(../img/fire.png)"} util/no-borders-style))

(defn select-type [style type fire?]
  (if (= type -1)
    style
    (let [img (str "url(../img/generated/cell_" type (if fire? "_fire" "") ".png)")]
      (assoc style :background-image img))))

(defn rotate [style angle]
  (assoc style :transform (str "rotate(" (* angle 90) "deg)"))
  )

(defn sprite [type angle fire?]
  [:div {:style (rotate (select-type base-style type fire?) angle)}]
  )

(defn selected-state [sprite selected?]
  (if selected? [:div {:style {:background-image "url(../img/generated/selected.png)"}} sprite] sprite))

(q/defcomponent
  ShuffleComponent [args] ;(selected time-to-reload)
  (html
    (let [selected? (:selected? args)
          time-to-reload (- 100 (:time-to-reload args))]
      (selected-state [:div {:style (assoc shuffle-style :opacity (/ time-to-reload 100) :filter (str "alpha(opacity=" time-to-reload "100)"))}] selected?))))

(q/defcomponent
  RocketComponent [args] ;[fire? fuel]
  (html [:div {:style {:background-image (str "url(../img/generated/fuel_" (:fuel args) ".png)")}}
         [:div {:style (if (:fire? args) rocket-fire-style rocket-style)}]
         ]))

(q/defcomponent
  EmptyComponent []
  (html [:div {:style empty-style}]))

(q/defcomponent
  FireComponent []
  (html [:div {:style fire-style}]))

(q/defcomponent
  CoolSpriteComponent [args]
  (html (selected-state (sprite (args :type) (args :angle) (args :fire?)) (args :selected?))))  ;CoolSpriteComponent [type, angle, fire?, selected?]