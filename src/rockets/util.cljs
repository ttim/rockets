(ns rockets.util)

(defn update-text
  [world-atom key value] (reset! world-atom (assoc @world-atom key value)))
