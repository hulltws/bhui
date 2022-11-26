(ns bh.ui-component.atom.worldwind.globe.image
  (:require ["worldwindjs" :as WorldWind]
            [bh.ui-component.atom.worldwind.globe.shape :as shape]
            [bh.ui-component.atom.worldwind.globe.sector :as sector]
            [bh.ui-component.utils.bounding-box :as bound]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.atom.worldwind.globe.image")


(defmethod shape/make-shape :shape/image [{:keys [id url bounding-box locations z]}]
  ;(log/info "shape/make-shape :shape/image" id url sector locations)

  (cond
    ; locations, but NOT bounding-box
    (and (seq locations)
      (not (seq bounding-box))) (let [bounding-box (bound/locations->bounding-box locations)
                                      surfaceImage (WorldWind/SurfaceImage. (sector/sector bounding-box) url)]
                                  (set! (.-resamplingMode surfaceImage) (.-FILTER_NEAREST WorldWind))

                                  (shape/wrap-shape id [surfaceImage] (or z 10)))

    ; bounding-box, but NOT locations
    (and (not (seq locations))
      (seq bounding-box)) (let [surfaceImage (WorldWind/SurfaceImage. (sector/sector bounding-box) url)]
                            (set! (.-resamplingMode surfaceImage) (.-FILTER_NEAREST WorldWind))
                            (shape/wrap-shape id [surfaceImage] (or z 10)))

    ; default to bounding-box, if it exists
    (seq bounding-box) (let [surfaceImage (WorldWind/SurfaceImage. (sector/sector bounding-box) url)]
                         (set! (.-resamplingMode surfaceImage) (.-FILTER_NEAREST WorldWind))
                         (shape/wrap-shape id [surfaceImage] (or z 10)))

    ; if BOTH are missing, "fail"
    :else [:div ":shape/image is missing both :bounding-box AND :locations parameters"]))



(comment
  (def bounding-box)



  ())