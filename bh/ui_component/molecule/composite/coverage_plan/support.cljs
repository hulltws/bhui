(ns bh.ui-component.molecule.composite.coverage-plan.support
  (:require [taoensso.timbre :as log]
            [bh.ui-component.atom.worldwind.globe.sector :as sector]
            [bh.ui-component.utils.bounding-box :as bound]
            [bh.ui-component.utils.helpers :as h]))


(log/info "bh.ui-component.molecule.composite.coverage-plan.support")


(def sensor-color-pallet [[:yellow "rgba(255, 255, 0, 0.3)" [255, 255, 0, 0.3] [1.0 1.0 0.0 0.2] "#FFFF00"]
                          [:cyan "rgba(0, 255, 255, 0.3)" [0, 255, 255, 0.3] [0.0 1.0 1.0 0.3] "#00FFFF"]
                          [:blue "rgba(0, 0, 255, 0.3)" [0, 0, 255, 0.3] [0.0 0. 1.0 0.1] "#0000FF"]
                          [:orange "rgba(255, 165, 0, 0.3)" [255, 165, 0, 0.3] [1.0 0.65 0.0 0.3] "#FFA500"]
                          [:grey "rgba(128, 128, 128, 0.3)" [128, 128, 128, 0.3] [0.5 0.5 0.5 0.3] "#808080"]
                          [:cornflowerblue "rgba(100, 149, 237, 0.3)" [100, 149, 237, 0.3] [0.4 0.58 0.93 0.3] "#6495ED"]
                          [:darkcyan "rgba(0, 139, 139, 0.3)" [0, 139, 139, 0.3] [0.0 0.55 0.55 0.3] "#008B8B"]
                          [:goldenrod "rgba(218, 165, 32, 0.3)" [218, 165, 32, 0.3] [0.84 0.65 0.13 0.3] "#DAA520"]
                          [:khaki "rgba(240, 230, 140, 0.3)" [240, 230, 140, 0.3] [0.94 0.90 0.55 0.3] "#F0E68C"]
                          [:deepskyblue "rgba(0, 191, 255, 0.3)" [0, 191, 255, 0.3] [1.0 0.0 1.0 0.3] "#00BFFF"]
                          [:darkseagreen "rgba(143, 188, 143, 0.3)" [143, 188, 143, 0.3] [0.55 0.74 0.56 0.3] "#8FBC8F"]
                          [:darkviolet "rgba(148, 0, 211, 0.3)" [148, 0, 211, 0.3] [0.58 0 0.83 0.3] "#9400D3"]
                          [:forestgreen "rgba(34, 139, 34, 0.3)" [34, 139, 34, 0.3] [0.13 0.55 0.13 0.3] "#228B22"]
                          [:orchid "rgba(218, 112, 214, 0.3)" [218, 112, 214, 0.3] [0.84 0.44 0.84 0.3] "#DA70D6"]
                          [:plum "rgba(221, 160, 221, 0.3)" [221, 160, 221, 0.3] [0.87 0.63 0.87 0.3] "#DDA0DD"]
                          [:tomato "rgba(255, 99, 71, 0.3)" [255, 99, 71, 0.3] [1.0 0.39 0.28 0.3] "#FF6347"]
                          [:green "rgba(0, 128, 0, 0.3)" [0, 128, 0, 0.3] [0.0 0.5 0.0 0.2] "#008000"]
                          [:orangered "rgba(255, 69, 0, 0.3)" [255, 69, 0, 0.3] [1.0 0.27 0.0 0.3] "#FF4500"]
                          [:navy "rgba(0, 0, 128, 0.3)" [0, 0, 128, 0.3] [0.0 0.0 0.5 0.3] "#000080"]
                          [:darkred "rgba(139, 0, 0, 0.3)" [139, 0, 0, 0.3] [0.55 0.0 0.0 0.3] "#8B0000"]])

(def num-rows 36)
(def num-cols 72)

(def row-size 5.0)
(def col-size 5.0)
(def cell-radius 150000)

(defn- get-cell-lat [[row _]]
  (- 90 (* row-size row)))


(defn- get-cell-lat-center [cell]
  (- (get-cell-lat cell) (/ row-size 2)))


(defn- get-cell-lon [[_ col]]
  (* col-size col))


(defn- get-cell-lon-center [cell]
  (+ (get-cell-lon cell) (/ col-size 2)))


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
      (for [row (range num-rows)
            col (range num-cols)]
        {[row col] [[(get-cell-lat [row col]) (get-cell-lon [row col])]
                    [(get-cell-lat [row (inc col)]) (get-cell-lon [row (inc col)])]
                    [(get-cell-lat [(inc row) (inc col)]) (get-cell-lon [(inc row) (inc col)])]
                    [(get-cell-lat [(inc row) col]) (get-cell-lon [(inc row) col])]
                    [(get-cell-lat [row col]) (get-cell-lon [row col])]]}))))


(def cell-centers
  (into (sorted-map-by <)
    (into {}
      (for [row (range num-rows)
            col (range num-cols)]
        {[row col] [(get-cell-lat-center [row col])
                    (get-cell-lon-center [row col])]}))))


(defn boundary-locations [cell]
  (get cell-boundaries cell))


(defn make-coverage-shape [{:keys [cell coverage time color] :as params}]
  ;(log/info "make-coverage-shape" cell coverage "//" color "//" (keys params))
  (let [[_ _ _ fill _] color
        [r g b a] fill
        outline [r g b (+ a 0.3)]
        ret     {:shape         :shape/polygon
                 :id            (clojure.string/join "-"
                                  [(:sensor coverage)
                                   time
                                   (str cell) r g b a])
                 :locations     (boundary-locations cell)
                 :width         2
                 :fill-color    fill
                 :outline-color outline
                 :z             500}]
    ;(log/info "make-coverage-shape (ret)" ret)
    ret))



(defn make-target-shape [[target-id row col ti color]]
  ;(log/info "make-target-shape" target-id color)
  (let [[_ _ _ c _] color
        [r g b _] c
        fill    [r g b 0.9]
        outline [r g b 1.0]]
    {:shape         :shape/circle
     :id            (clojure.string/join "-"
                      [target-id ti row col r g b])
     :location      (get cell-centers [row col])
     :radius        cell-radius
     :width         2
     :fill-color    fill
     :outline-color outline}))


(defmulti make-image-shape :type)


(defmethod make-image-shape :default [_]
  [:div])


(defmethod make-image-shape :points [{:keys [points] :as selected-imagery}]
  ;(log/info "make-image-shape :points" points)

  (map (fn [{:keys [id point]}]
         (let [[lat lon _] point]
           {:shape        :shape/image
            :id           id
            :url          "images/lightning/Lightning3png.png"
            :bounding-box (bound/make-bounding-box lat lon 0.5)
            :z            100}))
    points))


(defmethod make-image-shape :meso-image [{:keys [name bounding_box url] :as selected-imagery}]
  ;(log/info "make-image-shape :meso-image" selected-imagery)

  [{:shape        :shape/image
    :id           name
    :url          (str "cache/" url)
    :bounding-box bounding_box
    :z            50}])


(defmethod make-image-shape :fd-image [{:keys [name bounding_box url] :as selected-imagery}]
  ;(log/info "make-image-shape :fd-image" selected-imagery)

  [{:shape        :shape/image
    :id           name
    :url          (str "cache/" url)
    :bounding-box bounding_box
    :z            30}])


(defn make-imagery-shape [selected-imagery]
  ;(log/info "make-imagery-shape" selected-imagery)
  (let [ret (make-image-shape selected-imagery)]
    ;(log/info "make-imagery-shape (ret)" ret)
    ret))


(defn cook-coverages [satellites selected-satellites coverages current-time]
  ;(log/info "cook-coverages" satellites
  ;  "//" coverages
  ;  "//" current-time)

  (let [ret (->> coverages
              :data
              (filter #(= (:time %) current-time))
              (mapcat (fn [{:keys [coverage time cell computed_at color] :as all}]
                        (map (fn [c] {:time time :coverage c :cell cell :computed_at computed_at})
                          coverage)))
              (filter (fn [x]
                        (contains? selected-satellites
                          (get-in x [:coverage :sensor]))))
              (map (fn [cvg]
                     (let [platform  (get-in cvg [:coverage :platform])
                           sensor    (get-in cvg [:coverage :sensor])
                           satellite (first (filter #(and (= platform (:platform_id %))
                                                       (= sensor (:sensor_id %)))
                                              satellites))]
                       (assoc cvg :color (:color satellite))))))]

    ;(log/info "cook-coverages (ret)" ret)
    ret))


(defn cook-targets [targets selected-targets current-time]
  ;(log/info "cook-targets" targets "//" selected-targets "//" current-time)
  (let [ret (->> targets
              seq
              (mapcat (fn [{:keys [name cells color]}]
                        (map (fn [[r c ty ti]]
                               [name r c ti color])
                          cells)))
              (filter (fn [[id _ _ _ _]] (contains? selected-targets id)))
              (filter (fn [[_ _ _ ti _]] (= ti current-time))))]
    ;(log/info "cook-targets (ret)" ret)
    ret))


(defn cook-imagery [imagery selected-imagery]
  ;(log/info "cook-imagery" imagery "//" selected-imagery)
  (let [ret (->> imagery
              (filter (fn [m] (contains? selected-imagery (:name m)))))]
    ;(log/info "cook-imagery (ret)" ret)
    ret))


(comment
  (do
    (def targets [{:name  "alpha-hd", :cells #{[7 7 "hidef-image" 0] [7 6 "hidef-image" 1]
                                               [7 5 "hidef-image" 3] [7 6 "hidef-image" 2]},
                   :color [:darkred "rgba(139, 0, 0, .3)" [139 0 0 0.3] [0.55 0.0 0.0 0.1] "#8B0000"]}
                  {:name  "bravo-img", :cells #{[7 2 "image" 0] [7 1 "image" 1]},
                   :color [:blue "rgba(0, 0, 255, .3)" [0 0 255 0.3] [0 0 1 0.1] "#0000FF"]}
                  {:name  "fire-hd", :cells #{[5 3 "hidef-image" 2] [4 3 "hidef-image" 3]
                                              [4 3 "hidef-image" 2] [5 3 "hidef-image" 0] [5 3 "hidef-image" 3]},
                   :color [:orange "rgba(255, 165, 0, .3)" [255 165 0 0.3] [1 0.65 0 0.3] "#FFA500"]}
                  {:name  "fire-ir", :cells #{[5 4 "v/ir" 2] [5 4 "v/ir" 1] [5 3 "v/ir" 1]
                                              [5 4 "v/ir" 0] [5 4 "v/ir" 3]},
                   :color [:grey "rgba(128, 128, 128, .3)" [128 128 128 0.3] [0.5 0.5 0.5 0.3] "#808080"]}
                  {:name  "severe-hd", :cells #{[5 7 "hidef-image" 3] [5 6 "hidef-image" 0]
                                                [6 6 "hidef-image" 2] [6 5 "hidef-image" 1] [5 7 "hidef-image" 1]},
                   :color [:cornflowerblue "rgba(100, 149, 237, .3)"
                           [100 149 237 0.3] [0.4 0.58 0.93 0.3] "#6495ED"]}])
    (def selected-targets #{"bravo-img"})
    (def current-time 0))

  (->> targets
    seq
    (mapcat (fn [{:keys [name cells color]}]
              (map (fn [[r c ty ti]]
                     [name r c ti color])
                cells)))
    (filter (fn [[id _ _ _ _]] (contains? selected-targets id)))
    (filter (fn [[_ _ _ ti _]] (= ti current-time))))


  ())


(comment
  (do
    (def current-time 0)
    (def coverages [{:time        0
                     :cell        [9 7]
                     :coverage    #{{:platform "goes-west", :sensor "abi-3"} {:platform "goes-east", :sensor "abi-1"}},
                     :computed_at "2021-08-02T15:16:05.558813"}]))

  (->> coverages
    ;seq
    (filter #(= (:time %) current-time))
    (mapcat (fn [{:keys [coverage time cell computed_at] :as all}]
              (map (fn [c] {:time time :coverage c :cell cell :computed_at computed_at})
                coverage))))


  (do
    (def current-time 0)
    (def coverages [{:time        0
                     :cell        [9 7]
                     :coverage    #{{:platform "goes-west", :sensor "abi-3"} {:platform "goes-east", :sensor "abi-1"}},
                     :computed_at "2021-08-02T15:16:05.558813"}]))

  (->> coverages
    seq
    (filter #(= (:time %) current-time))
    (mapcat (fn [{:keys [coverage time cell computed_at] :as all}]
              (map (fn [c] {:time time :coverage c :cell cell :computed_at computed_at})
                coverage))))

  ())


(comment

  (do
    (def sample-shape {:shape      :shape/polygon :id "square"
                       :locations  [[30.0 -130.0] [30.0 -100.0]
                                    [0.0 -100.0] [0.0 -130.0]]
                       :fill-color [1 0 0 0.3] :outline-color [1 0 0 1] :width 2})
    (def coverage {:time        0
                   :cell        [9 7]
                   :coverage    {:platform "goes-west", :sensor "abi-3"}
                   :computed_at "2021-08-02T15:16:05.558813"}))


  (do
    (def id (clojure.string/join "-"
              [(get-in coverage [:coverage :platform])
               (:time coverage)
               (str (:cell coverage))]))
    (def locations (boundary-locations (:cell coverage)))
    (def fill-color (get-in sensor-color-pallet [0 2]))
    (def outline-color (get-in sensor-color-pallet [0 2]))
    (def width 2))

  {:shape      :shape/polygon :id id
   :locations  locations :width width
   :fill-color fill-color :outline-color outline-color}


  (def real-coverage [{:time        0
                       :cell        [9 7]
                       :coverage    #{{:platform "goes-west", :sensor "abi-3"} {:platform "goes-east", :sensor "abi-1"}},
                       :computed_at "2021-08-02T15:16:05.558813"}])

  (defn- make-one-shape [c]
    {:shape         :shape/polygon
     :id            (clojure.string/join "-"
                      [(get-in c [:coverage :platform])
                       (:time c)
                       (str (:cell c))])
     :locations     (boundary-locations (:cell c))
     :width         2
     :fill-color    (get-in sensor-color-pallet [0 2])
     :outline-color (get-in sensor-color-pallet [0 2])})

  (make-one-shape coverage)


  (make-coverage-shape real-coverage)





  ())


(comment
  (def selected-imagery [{:name   "glm",
                          :type   :points,
                          :points '([23.674062728881836 -85.5692138671875 112.0]
                                    [7.599512100219727 -76.88938903808594 206.0]
                                    [-5.726319313049316 -69.43939208984375 17.0])}
                         {:name         "abi-16-meso-1"
                          :type         :meso-image
                          :date-time    "20221006120000000"
                          :bounding-box [22.074653 33.344622 -95.433292 -82.723547]
                          :url          "20221006120000000.png"}])



  (map make-image-shape selected-imagery)


  (def points (:points (first selected-imagery)))

  (map (fn [[lat lon _]]
         {:id "dummy" :url "dummy" :bounding-box (sector/make-sector lat lon 0.5)})
    points)

  (mapcat (fn [{:keys [points]}]
            (map (fn [[lat lon _]]
                   {:id "dummy" :url "dummy" :bounding-box (sector/make-sector lat lon 0.1)})
              points))
    selected-imagery)


  (make-imagery-shape selected-imagery)

  ())


(comment
  (do
    (def cell [29 34])
    (def coverage {:platform "goes-16 (east)", :sensor "abi-16-meso-2"})
    (def time 0)
    (def color [:orange "rgba(255, 165, 0, .3)" [255 165 0 0.3] [1 0.65 0 0.3] "#FFA500"]))


  (let [[_ _ _ fill _] color
        [r g b a] fill
        outline [r g b (+ a 0.3)]
        ret     {:shape         :shape/polygon
                 :id            (clojure.string/join "-"
                                  [(:sensor coverage)
                                   time
                                   (str cell) r g b a])
                 :locations     (boundary-locations cell)
                 :width         2
                 :fill-color    fill
                 :outline-color outline}]
    {:fill fill :r r :g g :b b :a a :outline outline
     :ret  ret})

  (boundary-locations cell)


  ())


(comment
  (def row 7)
  (def col 5)
  [(get-cell-lat-center [row col])
   (get-cell-lon-center [row col])]


  ())




