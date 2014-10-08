(ns rockets.sprites
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]))

(def sprite-width 36)
(def rocket-height (* 2 sprite-width))

(defn rocket-style-fn
  [img-src] (merge {:width sprite-width :height rocket-height :background-image img-src} util/no-borders-style))
(def rocket-style (rocket-style-fn "url(img/generated/rocket.png)"))
(def rocket-fire-style (rocket-style-fn "url(img/generated/rocket_fire.png)"))

(def base-style (merge {:width sprite-width :height sprite-width} util/no-borders-style))

(def empty-style (merge base-style {:background-image "url(img/empty.png)"} util/no-borders-style))
(def shuffle-style (merge base-style {:background-image "url(img/generated/shuffle.png)"} util/no-borders-style))
(def fire-style (merge base-style {:background-image "url(img/fire.png)"} util/no-borders-style))

(defn select-type [style type fire?]
  (if (= type -1)
    style
    (let [img (str "url(img/generated/cell_" type (if fire? "_fire" "") ".png)")]
      (assoc style :background-image img))))

(defn rotate [style angle]
  (-> style
      (assoc :transform (str "rotate(" (* angle 90) "deg)"))
      (assoc :-webkit-transform (str "rotate(" (* angle 90) "deg)"))))

(defn sprite [type angle fire?]
  [:div {:style (rotate (select-type base-style type fire?) angle)}]
  )

(defn selected-state [sprite selected?]
  (if selected? [:div {:style {:background-image "url(img/generated/selected.png)"}} sprite] sprite))

(defn opacity [opacity]
  {:opacity (/ opacity 100) :filter (str "alpha(opacity=" opacity ")")})

(def names-style
  (merge {:font-family "'Geo', sans-serif" :color "white"} (opacity 10)))

(q/defcomponent
  ShuffleComponent [args]                                   ;(selected time-to-reload)
  (html
    (let [selected? (:selected? args)
          time-to-reload (- 100 (:time-to-reload args))]
      [:div {:style {:background-image "url(img/generated/bg.png)"}}
       (selected-state [:div {:style (merge shuffle-style (opacity time-to-reload))}] selected?)])))

(q/defcomponent
  RocketComponent [args]                                    ;[fire? fuel height]
  (let [height (min (:height args) rocket-height)
        pos (str "bottom " height "px right 0px")]
    (html [:div {:style {:height height, :background-position pos, :background-image (str "url(img/generated/fuel_" (:fuel args) ".png)")}}
           [:div {:style (assoc (if (:fire? args) rocket-fire-style rocket-style) :height height :background-position pos)}]
           ])))

(q/defcomponent
  EmptyComponent []
  (html [:div {:style empty-style}]))

(q/defcomponent
  EmptyWhiteComponent []
  (html [:div {:style {:background-color "white" :width "100%" :height "100%"}}]))

(q/defcomponent
  FireComponent []
  (html [:div {:style fire-style}]))

(q/defcomponent
  CoolSpriteComponent [args]
  (html [:div {:style {:background-image "url(img/generated/bg.png)"}}
         (selected-state (sprite (args :type) (args :angle) (args :fire?)) (args :selected?))])) ;CoolSpriteComponent [type, angle, fire?, selected?]

; start sprites framework
(defn create-sprites
  ([n m sprite-creator]
   (into [] (for [x (range 0 n)] (into [] (for [y (range 0 m)] (sprite-creator x y))))))
  ([n m] (create-sprites n m (fn [x y] nil))))

(defn sh [sprites] (count sprites))
(defn sw [sprites] (count (sprites 0)))

(defn merge-sprites [sprites upper-sprites offset-x offset-y]
  (create-sprites
    (sh sprites) (sw sprites)
    (fn [x y]
      (let [original ((sprites x) y)
            ux (- x offset-x)
            uy (- y offset-y)]
        (if (and (>= ux 0) (< ux (sh upper-sprites)))
          (if (and (>= uy 0) (< uy (sw upper-sprites)))
            ((upper-sprites ux) uy)
            original)
          original)))))

; args {:sprites, :zones}
(q/defcomponent
  SpritesComponent [args]
  (let [{:keys [sprites zones]} args]
    (html [:div {:style {
                          :border   "1px double white"
                          :position "relative"
                          :width    (* (sw sprites) sprite-width)
                          :height   (* (sh sprites) sprite-width)}}
           (for [zone zones
                 :let [{:keys [offset-x, offset-y, width, height, component]} zone]]
             [:div {:style {
                             :position "absolute"
                             :width    (* width sprite-width)
                             :height   (* height sprite-width)
                             :top      (* offset-x sprite-width)
                             :left     (* offset-y sprite-width)}} component])

           [:table {:style (merge util/no-borders-style {:position "absolute"})}
            (for [sprites-line sprites]
              [:tr {:style util/no-borders-style}
               (for [sprite sprites-line]
                 [:td {:style util/no-borders-style} (if (nil? sprite) (EmptyComponent) sprite)])])]
           ])))
; end sprites framework
