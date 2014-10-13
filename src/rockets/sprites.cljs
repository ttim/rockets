(ns rockets.sprites
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]))

(def sprite-width 36)
(def rocket-height (* 2 sprite-width))

; styles transformers
(defn with-dimensions [style width height]
  (merge style {:width width :height height}))
(defn with-image [style img-src]
  (assoc style :background-image img-src))
(defn rotate [style angle]
  (-> style
      (assoc :transform (str "rotate(" (* angle 90) "deg)"))
      (assoc :-webkit-transform (str "rotate(" (* angle 90) "deg)"))))
(defn opacity [style opacity]
  (-> style
      (assoc :opacity (/ opacity 100))
      (assoc :filter (str "alpha(opacity=" opacity ")"))))

; sprite styles
(def base-rocket-style (with-dimensions util/no-borders-style sprite-width rocket-height))
(def rocket-style (with-image base-rocket-style "url(img/generated/rocket.png)"))
(def rocket-fire-style (with-image base-rocket-style "url(img/generated/rocket_fire.png)"))

(def base-style (with-dimensions util/no-borders-style sprite-width sprite-width))
(def empty-style (with-image base-style "url(img/empty.png)"))
(def shuffle-style (with-image base-style "url(img/generated/shuffle.png)"))
(def fire-style (with-image base-style "url(img/fire.png)"))

(defn cell-img-src [type fire?]
  (str "url(img/generated/cell_" type (if fire? "_fire" "") ".png)"))

(defn select-type [style type fire?]
  (if (= type -1)
    style
    (assoc style :background-image (cell-img-src type fire?))))

(defn selected-state [sprite selected?]
  (if selected? [:div {:style {:background-image "url(img/generated/selected.png)"}} sprite] sprite))

(def names-style
  (opacity {:font-family "'Geo', sans-serif" :color "white"} 10))

(q/defcomponent
  ShuffleComponent [args]                                   ;(selected time-to-reload)
  (html
    (let [selected? (:selected? args)
          time-to-reload (- 100 (:time-to-reload args))]
      [:div {:style {:background-image "url(img/generated/bg.png)"}}
       (selected-state [:div {:style (opacity shuffle-style time-to-reload)}] selected?)])))

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
  CoolSpriteComponent [args]                                ;CoolSpriteComponent [type, angle, fire?, selected?]
  (html [:div {:style {:background-image "url(img/generated/bg.png)"}}
         (selected-state
           [:div {:style (rotate (select-type base-style (:type args) (:fire? args)) (:angle args))}]
           (:selected? args))]))

; start sprites framework
(def debug-sprites? (atom false))

(defrecord Sprites [height width zones])
(defrecord Zone [offset-top offset-left height width component])

(defn create-sprites
  ([height width] (->Sprites height width [])))
(defn single-sprite [component]
  (->Sprites 1 1 [(->Zone 0 0 1 1 component)]))

(defn add-zone [sprites offset-top offset-left height width component]
  (->Sprites (:height sprites) (:width sprites) (conj (:zones sprites) (->Zone offset-top offset-left height width component))))

(q/defcomponent
  TableComponent [args]
  (let [[height width cell-creator] args]
    (html [:div {:style {:position "relative"
                         :height   (* height sprite-width)
                         :width    (* width sprite-width)}}
           [:table {:style (merge util/no-borders-style {:position "absolute"})}
            (for [i (range 0 height)]
              [:tr {:style util/no-borders-style}
               (for [j (range 0 width)
                     :let [sprite (cell-creator i j)]]
                 [:td {:style util/no-borders-style}
                  (if (nil? sprite) (EmptyComponent) sprite)
                  ])])]
           ])))

(defn add-table [sprites cell-creator]
  (let [{:keys [height width]} sprites]
    (add-zone sprites 0 0 height width (TableComponent [height width cell-creator]))))

(q/defcomponent
  SpritesComponent [sprites]
  (let [{:keys [height width zones]} sprites
        debug-style (if @debug-sprites? {:border "1px double white"} {})]
    (html [:div {:style (merge {:position "relative"
                                :height   (* height sprite-width)
                                :width    (* width sprite-width)} debug-style)}
           (for [zone zones
                 :let [{:keys [offset-top, offset-left, height, width, component]} zone]]
             [:div {:style {
                             :position "absolute"
                             :height   (* height sprite-width)
                             :width    (* width sprite-width)
                             :top      (* offset-top sprite-width)
                             :left     (* offset-left sprite-width)}} component])
           ])))

; todo: what if upper-sprites will be more than sprites?
(defn add-sprites [sprites offset-top offset-left upper-sprites]
  (let [{:keys [height width]} upper-sprites]
    (add-zone sprites offset-top offset-left height width (SpritesComponent upper-sprites))))

; end sprites framework
