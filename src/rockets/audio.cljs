(ns rockets.audio)

(defn audio [name] (js/Audio. (str "sound/" name ".wav")))
(defn play! [audio] (. audio (play)))

(defonce rocket1 (audio "rocket1"))
(defonce rocket2 (audio "rocket2"))
(defonce rotate1 (audio "rotate1"))
(defonce rotate2 (audio "rotate2"))
(defonce shuffle (audio "shuffle"))

;(play! rotate1)
