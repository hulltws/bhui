(ns bh.ui-component.utils.bounding-box
  (:require [taoensso.timbre :as log]))


(log/info "bh.ui-component.utils.bounding-box")



(defn make-bounding-box [lat lon delta]
  [(- lat delta) (+ lat delta)
   (- lon delta) (+ lon delta)])



(defn locations->bounding-box [locations]
  (let [latitudes (map first locations)
        longitudes (map second locations)
        minLat (apply min latitudes)
        maxLat (apply max latitudes)
        minLon (apply min longitudes)
        maxLon (apply max longitudes)]
    [minLat maxLat minLon maxLon]))


(defn bounding-box->locations [[minLat maxLat minLon maxLon :as bounding-box]]
  [
   [minLat maxLon]
   [maxLat maxLon]
   [maxLat minLon]
   [minLat minLon]])










(comment

  (make-bounding-box -25.9087 -84.9876 0.5)

  (bounding-box->locations (make-bounding-box -25.9087 -84.9876 0.5))

  ())

