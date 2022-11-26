(ns bh.ui-component.atom.worldwind.globe.sector
  (:require ["worldwindjs" :as WorldWind]))


(defn sector
  ([[a b c d]]
   (WorldWind/Sector. a b c d))

  ([a b c d]
   (WorldWind/Sector. a b c d)))


