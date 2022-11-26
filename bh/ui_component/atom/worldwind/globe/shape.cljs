(ns bh.ui-component.atom.worldwind.globe.shape
  (:require ["worldwindjs" :as WorldWind]
            [bh.ui-component.atom.worldwind.globe.location :as location]
            [bh.ui-component.atom.worldwind.globe.shape.attributes :as attributes]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.atom.worldwind.globe.shape")


(defn wrap-shape [id shapes z]
  (log/info "wrap-shape" id "//" shapes)
  (let [layer (WorldWind/RenderableLayer.)]
    (set! (.-displayName layer) id)

    (dorun
      (map #(.addRenderable layer %) shapes))

    {:id id :layer layer :z z}))


(defmulti make-shape (fn [{:keys [shape]}]
                       ;(log/info "make-shape" shape)
                       shape))

; :shape/polygon
(defmethod make-shape :shape/polygon [{:keys [id locations
                                              fill-color outline-color
                                              width z]}]

  ;(log/info "polygon" locations "//" fill-color "//" outline-color "//" width)

  (let [attributes (attributes/shape-attributes
                     {:fill-color    fill-color
                      :outline-color outline-color
                      :width         width})
        locs       (->> locations
                     (map location/location)
                     (into-array))
        polygon    (WorldWind/SurfacePolygon. locs attributes)]
    (set! (.-displayName polygon) id)
    (wrap-shape id (vector polygon) (or z 5))))


; :shape/circle
(defmethod make-shape :shape/circle [{:keys [id location
                                             fill-color outline-color
                                             width radius z]}]

  ;(log/info "circle" location "//" fill-color "//" outline-color "//" width "//" radius)

  (let [attributes (attributes/shape-attributes
                     {:fill-color    fill-color
                      :outline-color outline-color
                      :width         width})
        circle     (WorldWind/SurfaceCircle. (location/location location)
                     radius attributes)]
    (set! (.-displayName circle) id)
    (wrap-shape id (vector circle) (or z 5))))


; :shape/polyline
(defmethod make-shape :shape/polyline [{:keys [id locations outline-color width z]}]

  ;(log/info "circle" locations "//" outline-color "//" width)

  (let [attributes (attributes/shape-attributes
                     {:outline-color outline-color
                      :width         width})
        locs       (->> locations
                     (map location/location)
                     (into-array))
        polyline   (WorldWind/SurfacePolyline. locs attributes)]
    (set! (.-displayName polyline) id)
    (wrap-shape id (vector polyline) (or z 5))))


;:shape/label
(defmethod make-shape :shape/label [{:keys [id label location fill-color outline-color width z]}]

  ;(log/info "label" location "//" label "//" fill-color "//" width)

  (let [label      (WorldWind/GeographicText. (location/position location) label)
        attributes (attributes/text-attributes
                     {:fill-color    fill-color
                      :outline-color outline-color
                      :width         width})]
    (set! (.-attributes label) attributes)
    (wrap-shape id (vector label) (or z 10))))



(comment
  (do
    (def locations [[-115.0 37.0] [-115.0 32.0]
                    [-107.0 33.0] [-102.0 31.0]
                    [-102.0 35.0] [-115.0 37.0]])
    (def fill-color [0.0 0.5 0.0 0.5])
    (def outline-color [0.0 0.5 0.0 1.0])
    (def width 5))

  (def attributes (attributes/attributes
                    {:fill-color    fill-color
                     :outline-color outline-color
                     :width         width}))
  (def locs (->> locations
              (map location/location)
              (into-array)))
  (def polygon (WorldWind/SurfacePolygon. locs attributes))


  ())


; refactor wrap-shape to work with collections, putting all the shapes on the same layer
(comment
  (do
    (def id "dummy")
    (def z 100)
    (def fill-color [0.0 0.5 0.0 0.5])
    (def outline-color [0.0 0.5 0.0 1.0])
    (def width 5)
    (def locations [[-115.0 37.0] [-115.0 32.0]
                    [-107.0 33.0] [-102.0 31.0]
                    [-102.0 35.0] [-115.0 37.0]])

    (def attr (attributes/shape-attributes
                {:fill-color    fill-color
                 :outline-color outline-color
                 :width         width}))
    (def locs (->> locations
                (map location/location)
                (into-array)))
    (def polygon (WorldWind/SurfacePolygon. locs attr))

    (set! (.-displayName polygon) id)
    (def shapes [polygon]))


  (def layer (WorldWind/RenderableLayer.))

  (map (fn [shape] {:shape shape}) shapes)

  (dorun
    (map #(.addRenderable layer %) shapes))
  (.-renderables layer)


  (let [attributes (attributes/shape-attributes
                     {:fill-color    fill-color
                      :outline-color outline-color
                      :width         width})
        locs       (->> locations
                     (map location/location)
                     (into-array))
        polygon    (WorldWind/SurfacePolygon. locs attributes)]
    (set! (.-displayName polygon) id))


  (let [layer (WorldWind/RenderableLayer.)]
    (set! (.-displayName layer) id)

    (dorun
      (map #(.addRenderable layer %) shapes))

    {:id id :layer layer :z z})



  ())
