(ns bh.ui-component.atom.worldwind.globe.globe-time
  (:require ["worldwindjs" :as WorldWind]
            [bh.ui-component.atom.worldwind.globe.layer :as l]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.atom.worldwind.globe.globe-time")


(defn change-time [this globe-id new-time]
  (if-let [layer (or (l/getLayer this (str globe-id " Night"))
                   (l/getLayer this (str globe-id " Day-only")))]
    (do
      ;(log/info "change-time" globe-id "//" (.-displayName layer) "//" new-time)
      (set! (.-time layer) new-time)
      (.redraw (.-wwd this)))))


