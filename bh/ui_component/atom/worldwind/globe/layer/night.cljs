(ns bh.ui-component.atom.worldwind.globe.layer.night
  (:require ["worldwindjs" :as WorldWind]
            [taoensso.timbre :as log]))


(defn night [layer-name]
  (let [layer (WorldWind/AtmosphereLayer.)]
    (set! (.-displayName layer) layer-name)
    (set! (.-opacity layer) 0.99)
    (set! (.-nightEnabled layer) true)
    layer))
