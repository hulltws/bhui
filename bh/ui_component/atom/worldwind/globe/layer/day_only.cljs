(ns bh.ui-component.atom.worldwind.globe.layer.day-only
  (:require ["worldwindjs" :as WorldWind]
            [taoensso.timbre :as log]))


(defn day-only [layer-name]
  (let [layer (WorldWind/AtmosphereLayer.)]
    (set! (.-displayName layer) layer-name)
    (set! (.-opacity layer) 0.6)
    (set! (.-nightEnabled layer) false)
    layer))
