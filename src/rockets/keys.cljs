(ns rockets.keys
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [chan put! pipe unique merge map< filter< alts!]]
            [goog.events :as events]
            [goog.dom :as gdom]
            [clojure.set :refer [union]]
            [clojure.string :as string]))

; based on https://github.com/joakin/cnake/blob/master/src/cnake/ui.cljs#L140-L217

;; -------------------------------------------------------------------------------
;; Key events handling

(def keycodes
  "Keycodes that interest us. Taken from
  http://docs.closure-library.googlecode.com/git/closure_goog_events_keynames.js.source.html#line33"
  {37 [:player2 :left]                                      ; left
   38 [:player2 :up]                                        ; up
   39 [:player2 :right]                                     ; right
   40 [:player2 :down]                                      ; down
   32 [:player2 :rotate]                                    ; space

   65 [:player1 :left]                                      ; a
   87 [:player1 :up]                                        ; w
   68 [:player1 :right]                                     ; d
   83 [:player1 :down]                                      ; s
   82 [:player1 :rotate]                                    ; r
   })

(defn event->key
  "Transform an js event object into the key name"
  [ev]
  ;(js/console.log (.-keyCode ev))
  (get keycodes (.-keyCode ev) :key-not-found))

(defn event-chan
  "Creates a channel with the events of type event-type and optionally applies
  the function parse-event to each event."
  ([event-type parse-event prevention-predicate]
   (let [ev-chan (chan)]
     (events/listen (.-body js/document)
                    event-type
                    #(let [prevent? (prevention-predicate)
                           key-code (.-keyCode %)]
                      (do
                        (when (and prevent? (contains? #{37 38 39 40 32} key-code))
                          (. % (preventDefault)))
                        (put! ev-chan (parse-event %)))))
     ev-chan)))

(defn keys-chan
  "Returns a channel with the key events of event-type parsed and
  filtered by the allowed-keys"
  [event-type prevention-predicate]
  (let [evs (event-chan event-type event->key prevention-predicate)]
    evs))

(defn keys-down-chan
  "Create a channel of keys pressed down restricted by the valid keys"
  [prevention-predicate] (keys-chan (.-KEYDOWN events/EventType) prevention-predicate))

(defn init-events!
  "Initialize event processing. It takes all the key presses and transforms
  them into commands and passes them to the game commands channel"
  [game-commands prevention-predicate]
  (let [keys-pressed (keys-down-chan prevention-predicate)]
    (pipe keys-pressed game-commands)))
