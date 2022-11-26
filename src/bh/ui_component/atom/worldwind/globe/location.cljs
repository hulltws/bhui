(ns bh.ui-component.atom.worldwind.globe.location
  (:require ["worldwindjs" :as WorldWind]))


(defn location
  ([[lat lon]]
   (WorldWind/Location. lat lon))

  ([lat lon]
   (WorldWind/Location. lat lon)))


(defn position
  ([[lat lon]]
   (WorldWind/Position. lat lon 100))

  ([lat lon]
   (WorldWind/Position. lat lon 100)))






