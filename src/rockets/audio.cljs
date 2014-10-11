(ns rockets.audio)

(def on? (atom true))
(def audios (atom []))

(defn audio [name]
  (let [res (js/Audio. (str "sound/" name ".wav"))]
    (reset! audios (conj @audios res))
    res))
(defn play! [audio]
  (when @on?
    (set! (.-currentTime audio) 0)
    (. audio (play))))
(defn stop! [audio]
  (do
    (. audio (pause))
    (set! (.-currentTime audio) 0)))

(add-watch on? ::audio
           (fn [_ _ old-data data] (when (and (not data) old-data) (doall (map stop! @audios)))))

(def rocket-sound (audio "rocket1"))
;(def rocket2-sound (audio "rocket2"))
(def rotate-sound (audio "rotate1"))
;(def rotate2-sound (audio "rotate2"))
(def shuffle-sound (audio "shuffle"))
(def win-sound (audio "tada"))

;(js/console.log (sablono.util/to-str audios))
