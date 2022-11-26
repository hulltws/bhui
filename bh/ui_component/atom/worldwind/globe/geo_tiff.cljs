(ns bh.ui-component.atom.worldwind.globe.geo-tiff
  (:require ["worldwindjs" :as WorldWind]
            [bh.ui-component.atom.worldwind.globe.shape :as shape]
            [bh.ui-component.atom.worldwind.globe.sector :as sector]
            [taoensso.timbre :as log]))

(log/info "bh.ui-component.atom.worldwind.globe.geo-tiff")


(def last-geoTiffReader (atom nil))
(def last-xhrStatus (atom nil))


(defmethod shape/make-shape :shape/geo-tiff [{:keys [id url z]}]
  ;(log/info "make-shape :shape/geo-tiff" id url)
  (let [layer (WorldWind/RenderableLayer.)]
    (set! (.-displayName layer) id)
    (set! (.-showSpinner layer) true)

    (.retrieveFromUrl (.-GeoTiffReader WorldWind) url
      (fn [geoTiffReader xhrStatus]
        (log/info "make-shape call-back" (.isGeoTiff geoTiffReader))
        (reset! last-geoTiffReader geoTiffReader)
        (let [surfaceTiff
              (new
                (.-SurfaceImage WorldWind)
                (.. geoTiffReader -metadata -bbox)
                ;(sector/sector [22.074653 33.344622 -95.433292 -82.723547]) ;
                (new (.-ImageSource WorldWind) (.getImage geoTiffReader)))]
          (log/info "make-shape surfaceTiff" surfaceTiff)
          (.addRenderable layer surfaceTiff)
          (set! (.-showSpinner layer) false))))

    {:id id :layer layer :z (or z 10)}))





(comment
  (def url "https://cdn.star.nesdis.noaa.gov/GOES18/ABI/FD/GEOCOLOR/GOES18-ABI-FD-GEOCOLOR-10848x10848.tif")
  (def url "https://worldwind.arc.nasa.gov/web/examples/data/black_sea_rgb.tif")
  (def url "data/20222351640_GOES18-ABI-FD-GEOCOLOR-10848x10848.jpg")
  (def url "data/20222351640_GOES18-ABI-FD-GEOCOLOR-10848x10848.tif")

  (def url "data/a.tif")


  (shape/make-shape {:shape :shape/geo-tiff
                     :id    "geo-tiff"
                     :url   url})


  (.. @last-geoTiffReader -metadata -bbox)
  (.getImage @last-geoTiffReader)
  (.-createImage @last-geoTiffReader)

  (.retrieveFromUrl (.-GeoTiffReader WorldWind) url
    (fn [geoTiffReader xhrStatus]
      (reset! last-xhrStatus xhrStatus)
      (reset! last-geoTiffReader geoTiffReader)))

  (WorldWind/SurfaceImage. (.. @last-geoTiffReader -metadata -bbox)
    (new (.-ImageSource WorldWind) (.getImage @last-geoTiffReader)))


  (new (.-ImageSource WorldWind) (.getImage @last-geoTiffReader))

  (WorldWind/SurfaceImage. (.. @last-geoTiffReader -metadata -bbox)
    "data/a.tif")


  (do
    (def meta-data (.. @last-geoTiffReader -metadata))
    (def bbox (.. @last-geoTiffReader -metadata -bbox))

    (def tiePointValues (.-modelTiepoint meta-data))
    (def modelPixelScaleValues (.-modelPixelScale meta-data))
    (def modelTransformationValues (.-modelTransformation meta-data))

    (def tiePointCount (if tiePointValues (.-length tiePointValues) 0))
    (def modelPixelScaleCount (if modelPixelScaleValues (.-length modelPixelScaleValues) 0))
    (def modelTransformationCount (if modelTransformationValues
                                    (.-length modelTransformationValues) 0)))

  (.-imageLength meta-data)
  (.-imageWidth meta-data)
  (.-imageDescription meta-data)
  (.-metaData meta-data)



  (.-minLatitude bbox)
  (.-maxLatitude bbox)
  (.-minLongitude bbox)
  (.-maxLongitude bbox)
  (.deltaLatitude bbox)
  (.deltaLongitude bbox)

  ())

