(ns bh.ui-component.atom.worldwind.globe.layer.coordinates
  (:require ["worldwindjs" :as WorldWind]
            [taoensso.timbre :as log]))


(defn coordinates [this layer-name]
  (let [layer (WorldWind/CoordinatesDisplayLayer. (.-wwd this))]
    (set! (.-displayName layer) layer-name)
    layer))