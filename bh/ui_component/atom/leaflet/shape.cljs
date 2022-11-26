(ns bh.ui-component.atom.leaflet.shape
  (:require ["react-leaflet" :refer (Polygon Circle Polyline ImageOverlay Marker Popup)]
            [bh.ui-component.utils.color :as c]
            [bh.ui-component.utils.bounding-box :as bound]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.atom.leaflet.shape")


(defmulti make-shape (fn [{:keys [shape]}] shape))


; :shape/polygon
(defmethod make-shape :shape/polygon [{:keys [id locations fill-color outline-color width]}]
  ;(log/info "make-shape :shape/polygon")

  [:> Polygon {:pathOptions {:color (-> outline-color c/normal->rgba c/rgba->hex)
                             :weight width
                             :fillColor (-> fill-color c/normal->rgba c/rgba->hex)}
               :positions locations}])


; :shape/polyline
(defmethod make-shape :shape/polyline [{:keys [id locations outline-color width]}]
  ;(log/info "make-shape :shape/polyline")

  [:> Polyline {:pathOptions {:color (-> outline-color c/normal->rgba c/rgba->hex)
                              :weight width}
                :positions locations}])


; :shape/circle
(defmethod make-shape :shape/circle [{:keys [id location radius fill-color outline-color width]}]
  ;(log/info "make-shape :shape/circle")

  [:> Circle {:pathOptions {:color (-> outline-color c/normal->rgba c/rgba->hex)
                            :fillColor (-> fill-color c/normal->rgba c/rgba->hex)
                            :weight width}
              :radius radius
              :center location}])


; :shape/image
(defmethod make-shape :shape/image [{:keys [id locations bounding-box url]}]
  ;(log/info "make-shape :shape/image")

  [:> ImageOverlay {:bounds (cond
                              (seq locations) locations
                              (seq bounding-box ) (bound/bounding-box->locations bounding-box)
                              :else [:div ":shape/image is missing both :bounding-box AND :locations parameters"])
                    :url url}])


; :shape/label
(defmethod make-shape :shape/label [{:keys [id location label]}]
  ;(log/info "make-shape :shape/label")

  [:> Marker {:position location}
   [:> Popup label]])



(defmethod make-shape :default [_]
  [:div])


(comment
  (def outline-color [1 0.5 0.78 1.0])



  ())