(ns bh.ui-component.atom.worldwind.globe.layer.controls
  (:require ["worldwindjs" :as WorldWind]
            [taoensso.timbre :as log]))


(def last-this (atom nil))


(defn controls [this layer-name]
  (reset! last-this this)

  (let [layer (WorldWind/ViewControlsLayer. (.-wwd this))]
    (set! (.-displayName layer) layer-name)
    ;(set! (.-placement layer) (WorldWind/Offset. WorldWind/OFFSET_PIXELS 11
    ;                            WorldWind/OFFSET_PIXELS 11))
    ;(set! (.-showPanControl this) false)
    ;(set! (.-showHeadingControl this) false)
    ;(set! (.-showTiltControl this) false)
    ;(set! (.-showZoomControl this) false)
    ;(set! (.-showExaggerationControl this) false)
    ;(set! (.-showFieldOfViewControl this) false)
    layer))



(comment
  (def layer (WorldWind/ViewControlsLayer. (.-wwd @last-this)))


  ())