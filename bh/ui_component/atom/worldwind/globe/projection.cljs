(ns bh.ui-component.atom.worldwind.globe.projection
  (:require ["worldwindjs" :as WorldWind]))


(def projections ["3D",
                  "Equirectangular",
                  "Mercator",
                  "North Polar",
                  "South Polar",
                  "North UPS",
                  "South UPS",
                  "North Gnomonic",
                  "South Gnomonic"])


(defn change-projection [this new-projection]
  ;(log/info "change-projection" new-projection
  ;  "//// roundGlobe" (.-roundGlobe this)
  ;  "//// flatGlobe " (.-flatGlobe this))

  (if (= "3D" new-projection)
    (do
      ;(log/info "changing to roundGlobe")
      (if (not (.-roundGlobe this))
        (set! (.-roundGlobe this) (WorldWind/Globe. (WorldWind/EarthElevationModel.))))

      ; Replace the flat globe
      (if (not= (.. this -wwd -globe) (.-roundGlobe this))
        (do
          ;(log/info "setting the roundGlobe" (.-roundGlobe this))
          (set! (.. this -wwd -globe) (.-roundGlobe this))
          (.redraw (.-wwd this)))))

    (do
      ;(log/info "changing to flatGlobe")

      (if (not (.-flatGlobe this))
        (set! (.-flatGlobe this) (WorldWind/Globe2D.)))

      (set! (.. this -flatGlobe -projection)
        (condp = new-projection
          "Equirectangular" (WorldWind/ProjectionEquirectangular.)
          "Mercator" (WorldWind/ProjectionMercator.)
          "North Polar" (WorldWind/ProjectionPolarEquidistant. "North")
          "South Polar" (WorldWind/ProjectionPolarEquidistant. "South")
          "North UPS" (WorldWind/ProjectionUPS. "North")
          "South UPS" (WorldWind/ProjectionUPS. "South")
          "North Gnomonic" (WorldWind/ProjectionGnomonic. "North")
          "South Gnomonic" (WorldWind/ProjectionGnomonic. "South")))

      (if (not= (.. this -wwd -globe) (.-flatGlobe this))
        (do
          ;(log/info "setting the flatGlobe" (.-flatGlobe this))
          (set! (.. this -wwd -globe) (.-flatGlobe this))
          (.redraw (.-wwd this)))))))

