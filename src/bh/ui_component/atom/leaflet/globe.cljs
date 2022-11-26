(ns bh.ui-component.atom.leaflet.globe
  (:require ["react-leaflet" :refer (MapContainer TileLayer)]
            [bh.ui-component.utils.helpers :as h]
            [bh.ui-component.atom.leaflet.shape :as s]
            [bh.ui-component.utils.bounding-box :as bound]
            [re-frame.core :as rf]
            [cljs-time.coerce :as coerce]
            [cljs-time.core :as cljs-time]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.atom.leaflet.globe")


(def sample-data [
                  {:shape      :shape/polygon :id "square"
                   :locations [[30.0 -130.0] [30.0 -100.0]
                               [0.0 -100.0] [0.0 -130.0]]
                   :fill-color [1 0 0 0.3] :outline-color [1 0 0 1] :width 2}
                  {:shape      :shape/polygon :id "5-sided"
                   :locations [[37 -115.0] [32.0 -115.0] [33.0 -107.0]
                               [31.0 -102.0] [35.0 -102.0] [37.0 -115.0]]
                   :fill-color [1 0 0 0.6] :outline-color [1 0 0 1] :width 2}
                  {:shape :shape/polyline :id "line1" :locations [[35 -75] [35 -125]]
                   :outline-color [1 1 0 1.0] :width 5}
                  {:shape      :shape/circle :id "circle"
                   :location [28.538336 -81.379234] :radius 1000000
                   :fill-color [0 1 0 0.5] :outline-color [1 1 1 1] :width 2 :height 2}
                  {:shape :shape/polyline :id "line2" :locations [[22 -55] [45 -105] [36 -125.7]]
                   :outline-color [1 0.5 0.78 1.0] :width 5}
                  {:shape :shape/label :id "orlando" :location [28.538336 -81.379234] :label "Orlando"
                   :fill-color [1 0.9 0.0 1.0] :outline-color [1 0.9 0.0 1.0] :width 1}

                  {:shape :shape/image, :id "image-15",
                   :url   "data/a.png",
                   :locations [[22.229767 -93.016231]
                               [33.344622 -95.433292]
                               [33.082839 -83.694864]
                               [22.074653 -82.723547]]}

                  {:shape :shape/image
                   :id (h/component-id)
                   :url "images/lightning/Lightning3png.png"
                   :bounding-box (bound/make-bounding-box -26.076 -85.876 0.5)}
                  {:shape :shape/image
                   :id (h/component-id)
                   :url "images/lightning/Lightning3png.png"
                   :bounding-box (bound/make-bounding-box -25.9087 -84.9876 0.5)}
                  {:shape :shape/image
                   :id (h/component-id)
                   :url "images/lightning/Lightning3png.png"
                   :bounding-box (bound/make-bounding-box -25.0987 -86.09 0.5)}
                  {:shape :shape/image
                   :id (h/component-id)
                   :url "images/lightning/Lightning3png.png"
                   :bounding-box (bound/make-bounding-box -26 -85 0.5)}])



(defn globe [& {:keys [shapes current-time component-id container-id]}]
  ;[:div "a leaflet-based 'React' \"globe\""]

  (let [s (h/resolve-value shapes)
        t (h/resolve-value current-time)]
    ;(log/info "globe OUTER" shapes component-id)
    (fn []
     [:div {:style {:width "100%" :height "100%"}}
      [:> MapContainer {:center [32, -82]  :zoom 2 :scrollWheelZoom false}
       [:> TileLayer {:url "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                      :attribution "&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors"}]
       (into [:<>]
         (doall (map s/make-shape @s)))]])))


(def meta-data {:l/globe {:component globe
                          :ports     {:shapes       :port/sink
                                      :current-time :port/sink}}})


(rf/dispatch-sync [:register-meta meta-data])
