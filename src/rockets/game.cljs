(ns rockets.game
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]))

(q/defcomponent
  GameComponent [data world-atom]
  (html
    [:h1 "Game Not Impplemented"]
    ))
