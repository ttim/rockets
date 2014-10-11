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
(def debug-sprites? (atom false))
(defn debug-sprites! [debug?]
  (reset! debug-sprites? debug?))

(defrecord Sprites [n m zones])
(defrecord Zone [offset-x offset-y width height component])

(defn create-sprites
  ([n m] (->Sprites n m [])))
(defn single-sprite [component]
  (->Sprites 1 1 [(->Zone 0 0 1 1 component)]))

(defn add-zone [sprites offset-x offset-y width height component]
  (->Sprites (:n sprites) (:m sprites) (conj (:zones sprites) (->Zone offset-x offset-y width height component))))

(q/defcomponent
  TableComponent [args]
  (let [[n m cell-creator] args]
    (html [:div {:style {:position "relative"
                         :width    (* m sprite-width)
                         :height   (* n sprite-width)}}
           [:table {:style (merge util/no-borders-style {:position "absolute"})}
            (for [i (range 0 n)]
              [:tr {:style util/no-borders-style}
               (for [j (range 0 m)
                     :let [sprite (cell-creator i j)]]
                 [:td {:style util/no-borders-style}
                  (if (nil? sprite) (EmptyComponent) sprite)
                  ])])]
           ])))

(defn add-table [sprites cell-creator]
  (let [{:keys [n m]} sprites]
    (add-zone sprites 0 0 n m (TableComponent [n m cell-creator]))))

(q/defcomponent
  SpritesComponent [sprites]
  (let [{:keys [n m zones]} sprites
        debug-style (if @debug-sprites? {:border "1px double white"} {})]
    (html [:div {:style (merge {:position "relative"
                                :width    (* m sprite-width)
                                :height   (* n sprite-width)} debug-style)}
           (for [zone zones
                 :let [{:keys [offset-x, offset-y, width, height, component]} zone]]
             [:div {:style {
                             :position "absolute"
                             :width    (* width sprite-width)
                             :height   (* height sprite-width)
                             :top      (* offset-x sprite-width)
                             :left     (* offset-y sprite-width)}} component])
           ])))

; todo: what if upper-sprites will be more than sprites?
(defn add-sprites [sprites offset-x offset-y upper-sprites]
  (let [{:keys [n m]} upper-sprites]
    (add-zone sprites offset-x offset-y n m (SpritesComponent upper-sprites))))

; end sprites framework
