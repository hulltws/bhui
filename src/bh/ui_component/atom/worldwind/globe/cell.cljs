(ns bh.ui-component.atom.worldwind.globe.cell
  (:require ["worldwindjs" :as WorldWind]))


(defn- get-cell-lat [[row _]]
  ;(* (- 4.5 row) 20.0))
  (- 60 (* 6.5 row)))


(defn- get-cell-lat-center [cell]
  ;(- (get-cell-lat cell) 10.0))
  (- (get-cell-lat cell) 3))


(defn- get-cell-lon [[_ col]]
  ;(* (- 9 col) -20.0))
  (+ -60 (* -12 (- 9 col))))


(defn- get-cell-lon-center [cell]
  ;(+ (get-cell-lon cell) 10.0))
  (+ (get-cell-lon cell) 6.0))


; pre-gen all the cell boundaries as [lat lon] pairs, and group together for
; a complete "polygon":
;
;       (1)          (2)
; (5)  [0 0] -----> [0 1]
;        ^            |
;        |            v
;      [1 0] -----> [1 1]
;       (4)          (3)
;
(def cell-boundaries
  (into (sorted-map-by <)
    (into {}
      (for [row (range 10)
            col (range 10)]
        {[row col] [[(get-cell-lat [row col]) (get-cell-lon [row col])]
                    [(get-cell-lat [row (inc col)]) (get-cell-lon [row (inc col)])]
                    [(get-cell-lat [(inc row) (inc col)]) (get-cell-lon [(inc row) (inc col)])]
                    [(get-cell-lat [(inc row) col]) (get-cell-lon [(inc row) col])]
                    [(get-cell-lat [row col]) (get-cell-lon [row col])]]}))))


(def cell-centers
  (into (sorted-map-by <)
    (into {}
      (for [row (range 10)
            col (range 10)]
        {[row col] [(get-cell-lat-center [row col])
                    (get-cell-lon-center [row col])]}))))


(defn boundary-locations [cell]
  (let [poly-points (get cell-boundaries cell)]
    (->> poly-points
      (map (fn [[lat lon]]
             (WorldWind/Location. lat lon)))
      (into-array))))

