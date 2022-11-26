(ns bh.ui-component.atom.worldwind.globe.color
  (:require ["worldwindjs" :as WorldWind]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.atom.worldwind.globe.color")


(def default-fill-color [0.0 0.5 0.0 0.3])
(def default-outline-color [0.0 0.5 0.0 1.0])
(def default-width 2)


(def color-pallet [[:green "rgba(0, 128, 0, .3)" [0.0 0.5 0.0 0.3]]
                   [:blue "rgba(0, 0, 255, .3)" [0.0 0. 1.0 0.3]]
                   [:orange "rgba(255, 165, 0, .3)" [1.0 0.65 0.0 0.3]]
                   [:grey "rgba(128, 128, 128, .3)" [0.5 0.5 0.5 0.3]]
                   [:cornflowerblue "rgba(100, 149, 237, .3)" [0.4 0.58 0.93 0.3]]
                   [:darkcyan "rgba(0, 139, 139, .3)" [1.0 0.55 0.55 0.3]]
                   [:goldenrod "rgba(218, 165, 32, .3)" [0.84 0.65 0.13 0.3]]
                   [:khaki "rgba(240, 230, 140, .3)" [0.94 0.90 0.55 0.3]]
                   [:deepskyblue "rgba(0, 191, 255, .3)" [1.0 0.0 1.0 0.3]]
                   [:navy "rgba(0, 0, 128, .3)" [0.0 0.0 0.5 0.3]]
                   [:cyan "rgba(0, 255, 255, .3)" [0.0 1.0 1.0 0.9]]
                   [:darkred "rgba(139, 0, 0, .3)" [0.55 0.0 0.0 0.3]]
                   [:darkseagreen "rgba(143, 188, 143, .3)" [0.55 0.74 0.56 0.3]]
                   [:darkviolet "rgba(148, 0, 211, .3)" [0.58 0 0.83 0.3]]
                   [:forestgreen "rgba(34, 139, 34, .3)" [1.0 0.71 0.76 0.9]]
                   [:orchid "rgba(218, 112, 214, .3)" [0.84 0.44 0.84 0.3]]
                   [:plum "rgba(221, 160, 221, .3)" [0.87 0.63 0.87 0.9]]
                   [:tomato "rgba(255, 99, 71, .3)" [1.0 0.39 0.28 0.3]]
                   [:orangered "rgba(255, 69, 0, .3)" [1.0 0.27 0.0 0.3]]])


; colors
(defn yellow [alpha] [128 128 0 alpha])
(defn white [alpha] [255 255 255 alpha])
(defn blue [alpha] [0 0 255 alpha])
(defn red [alpha] [255 0 0 alpha])
(defn green [alpha] [0 255 0 alpha])
(defn black [alpha] [0 0 0 alpha])


(defn color
  ([r g b a]
   ;(log/info "color" r g b a)
   (WorldWind/Color. r g b a))

  ([[r g b a]]
   ;(log/info "color" r g b a)
   (WorldWind/Color. r g b a)))
