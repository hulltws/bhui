(ns bh.ui-component.atom.resium.shape
  (:require ["resium" :refer (Viewer CameraFlyTo Globe Entity EllipseGraphics PolygonGraphics PolylineGraphics PointPrimitive LabelGraphics)]
            ["cesium" :refer (Cartesian3 Ion Color CircleGeometry LabelStyle Material MaterialProperty ImageMaterialProperty)]
            [bh.ui-component.utils.bounding-box :as bound]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.atom.resium.shape")


(defn- correct-locations
  "Cesium/Resium locations are [lon lat] while Worldwind locations are [lat lon], so we need this
  function to do the conversion (easier to go in this direction)
  "
  [locations]
  (->> locations
    (map (fn [[lat lon]] [lon lat]))
    flatten))


(defn- correct-location [[lat lon]]
  [lon lat])


(defn- cartesian3
  ([[lon lat]] (.fromDegrees Cartesian3 lon lat))

  ([lon lat] (.fromDegrees Cartesian3 lon lat)))


(defmulti make-shape (fn [{:keys [shape]}] shape))

; :shape/polygon
(defmethod make-shape :shape/polygon [{:keys [id locations fill-color outline-color width]}]
  (let [[f-r f-g f-b f-a] fill-color
        [o-r o-g o-b o-a] outline-color]
    ^{:key id} [:> Entity
                [:> PolygonGraphics {:hierarchy    (.fromDegreesArray Cartesian3 (clj->js (correct-locations locations)))
                                     :outlineColor (Color. o-r o-g o-b o-a)
                                     :outlineWidth width
                                     :outline      true
                                     :material     (Color. f-r f-g f-b f-a)}]]))


; :shape/image
(defmethod make-shape :shape/image [{:keys [id bounding-box locations url]}]
  ;(log/info "make-shape" id bounding-box locations url)
  ^{:key id} [:> Entity
              [:> PolygonGraphics {:hierarchy (.fromDegreesArray Cartesian3
                                                (clj->js (correct-locations (cond
                                                                              (seq locations) locations
                                                                              (seq bounding-box) (bound/bounding-box->locations bounding-box)
                                                                              :else [:div ":shape/image is missing both :bounding-box AND :locations parameters"]))))
                                   :material (ImageMaterialProperty. (clj->js {:image url}))}]])


; :shape/polyline
(defmethod make-shape :shape/polyline [{:keys [id locations width outline-color]}]
  (let [[r g b a] outline-color]
    ^{:key id} [:> Entity
                [:> PolylineGraphics {:positions (.fromDegreesArray Cartesian3 (clj->js (correct-locations locations)))
                                      :width     width
                                      :material  (Color. r g b a)}]]))


; :shape/circle
(defmethod make-shape :shape/circle [{:keys [id location radius fill-color outline-color width height]}]
  (let [[f-r f-g f-b f-a] fill-color
        [o-r o-g o-b o-a] outline-color]
    ^{:key id} [:> Entity {:position (cartesian3 (correct-location location))}
                [:> EllipseGraphics {:semiMajorAxis radius
                                     :semiMinorAxis radius
                                     :material      (Color. f-r f-g f-b f-a)
                                     :outlineColor  (Color. o-r o-g o-b o-a)
                                     :outlineWidth  width
                                     :height        height
                                     :outline       true}]]))


; :shape/label
(defmethod make-shape :shape/label [{:keys [id location label font fill-color outline-color width]}]
  (let [[f-r f-g f-b f-a] fill-color
        [o-r o-g o-b o-a] outline-color]
    ^{:key id}  [:> Entity {:position (cartesian3 (correct-location location))}
                 [:> LabelGraphics {:text         label
                                    :font         (or font "24px Helvetica")
                                    :fillColor    (Color. f-r f-g f-b f-a)
                                    :outlineColor (Color. o-r o-g o-b o-a)
                                    :outlineWidth width
                                    :show         true}]]))

(defmethod make-shape :default [_]
  [:> Entity])
