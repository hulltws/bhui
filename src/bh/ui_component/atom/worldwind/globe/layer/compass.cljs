(ns bh.ui-component.atom.worldwind.globe.layer.compass
  (:require ["worldwindjs" :as WorldWind]
            [taoensso.timbre :as log]))


(defn compass [layer-name]
  (let [layer (WorldWind/CompassLayer.)]
    (set! (.-displayName layer) layer-name)
    layer))
