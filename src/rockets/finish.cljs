(ns rockets.finish
  (:require
    [sablono.core :as html :refer-macros [html]]
    [quiescent :as q :include-macros true]
    [clojure.string :as string]
    [rockets.util :as util]))

(q/defcomponent
  FinishComponent [data world-atom]
  (html
    [:h1 "Finish Not Impplemented"]
    ))
