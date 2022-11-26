(ns bh.ui-component.atom.worldwind.globe.layer
  (:require ["worldwindjs" :as WorldWind]
            [taoensso.timbre :as log]))


(defn renderable-layer [layer-name children]
  (let [layer (WorldWind/RenderableLayer.)]
    (set! (.-displayName layer) layer-name)
    (doall
      (map (fn [child]
             ;(log/info "renderable-layer adding" layer-name child)
             (.addRenderable layer child))
        children))
    layer))


(defn getLayer [this layer-name]
  (let [layer (.filter (.. this -wwd -layers)
                #(= (.-displayName %) layer-name))]
    ;(log/info "getLayer"
    ;  (.-length layer)
    ;  (map #(.-displayName %) layer))
    (first layer)))


(defn addLayer [this idx layer]
  ;(log/info "addLayer" (.-displayName layer))

  (.insertLayer (.-wwd this) idx layer)
  (.redraw (.-wwd this))

  layer)


(defn removeLayer [this layer-name]
  ;(log/info "removeLayer" layer-name)

  (if-let [layer (getLayer this layer-name)]
    (do
      (.removeLayer (.-wwd this) layer)
      ;(log/info "removed?" (map #(.-displayName %)
      ;                       (.. this -wwd -layers)))
      (.redraw (.-wwd this)))))

