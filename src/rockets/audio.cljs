(ns rockets.audio)

(def audios)
(def audio-world)

(defn audio [name]
  (let [res (js/Audio. (str "sound/" name ".wav"))
        old-audios audios]
    (def audios (conj old-audios res))
    res))
(defn play! [audio]
  (let [audio? (:audio? @audio-world)]
    (when audio? (. audio (play)))))
(defn stop! [audio]
  (do
    (. audio (pause))
    (set! (.-currentTime audio) 0)))


(defn init! [world-atom]
  (def audio-world world-atom)
  (add-watch
    audio-world ::audio
    (fn [_ _ old-data data]
      (when (and (not (:audio? data)) (:audio? old-data))
        (doall (map stop! audios))))))

(defonce rocket1-sound (audio "rocket1"))
(defonce rocket2-sound (audio "rocket2"))
(defonce rotate1-sound (audio "rotate1"))
(defonce rotate2-sound (audio "rotate2"))
(defonce shuffle-sound (audio "shuffle"))

;(js/console.log (sablono.util/to-str audios))