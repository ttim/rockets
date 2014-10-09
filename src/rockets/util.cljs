(ns rockets.util
  (:require
    [clojure.browser.dom :as dom]
    [quiescent :as q :include-macros true]))

(defn update-text
  [world-atom key value] (reset! world-atom (assoc @world-atom key value)))

(defn update-state-log
  [state element] (dom/set-text element (sablono.util/to-str state)))
(defn bind-state-log
  [state-atom element]
  (add-watch
    state-atom ::state-log-render
    (fn [_ _ _ data] (update-state-log data element)))
  (defonce _first_time_log_render (update-state-log @state-atom element)))

(def no-borders-style {:border-width 0, :padding "0 0 0 0", :margin "0 0 0 0", :border-spacing 0})

(defn redirect-to [url]
  (set! (.-href js/window.location) url))

(def clojurecup-app-url "https://clojurecup.com/#/apps/rockets")

(def twitter-share-url
  (let [text "Check%20out%20Awesome%20Rocketeers%20game%20https%3A%2F%2Fclojurecup.com%2F%23%2Fapps%2Frockets%20%23clojurecup%20%23rocketeers%0A"]
    (str "https://twitter.com/intent/tweet?text=" text)))

(defn log [obj] (js/console.log (sablono.util/to-str obj)))

(def enabled-time-debug-keys #{
                               ;:render
                               ;:build-sprites
                               })

(defn current-time-ms [] (. (js/Date.) (getTime)))
(defn with-time-debug [key fn]
  (if (contains? enabled-time-debug-keys key)
    #(let [start-time (current-time-ms)
           result (fn)
           finish-time (current-time-ms)]
      (log (str key " " (- finish-time start-time)))
      result)
    fn))

; render
(def last-rendered-state (atom {}))
(defn render-if-needed [key atom dom-element component-builder]
  (when-not (= (@last-rendered-state key) @atom)
    (q/render (component-builder) dom-element)
    (reset! last-rendered-state (assoc @last-rendered-state key @atom))))

(defn render! [key atom dom-element component-builder]
  (let [render-func (with-time-debug :render #(render-if-needed key atom dom-element component-builder))]
    (render-func)
    (add-watch
      atom key
      (fn [_ _ _ data] (if (exists? js/requestAnimationFrame)
                         (js/requestAnimationFrame render-func)
                         (js/setTimeout render-func 16))))))
