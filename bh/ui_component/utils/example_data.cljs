(ns bh.ui-component.utils.example-data
  (:require [bh.subs :as subs]
            [cljs-uuid-utils.core :as uuid]
            [cljs.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [expound.alpha :as expound]
            [malli.core :as m]
            [malli.error :as me]
            [malli.generator :as mg]
            [malli.provider :as mp]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.utils.example-data")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; SOME EXAMPLE DATA
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; Chain-of-Custody data
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; region
(def default-coc [{:coc/step      :generated
                   :coc/by        "bh.ui-component.atom.bh.table"
                   :coc/version   "no version"
                   :coc/at        (str (js/Date.))
                   :coc/signature (uuid/uuid-string (uuid/make-random-uuid))}])

; in clojure.spec (2)
(spec/def :coc/step (spec/and #{:generated :updated :deleted}))
(spec/def :coc/by string?)
(spec/def :coc/version string?)
(spec/def :coc/at string?)
(spec/def :coc/signature string?)
(spec/def :coc/entry (spec/keys :req [:coc/step :coc/by :coc/version
                                      :coc/at :coc/signature]))
(spec/def :coc/coc (spec/coll-of :coc/entry))

(comment
  (spec/valid? :coc/entry {:coc/step      :generated
                           :coc/by        "bh.ui-component.atom.bh.table"
                           :coc/version   "no version"
                           :coc/at        (str (js/Date.))
                           :coc/signature (uuid/uuid-string (uuid/make-random-uuid))})
  (spec/valid? :coc/entry {:coc/step      :generated
                           :coc/by        "bh.ui-component.atom.bh.table"
                           :coc/version   "no version"
                           :coc/at        (js/Date.)
                           :coc/signature (uuid/make-random-uuid)})
  (spec/explain :coc/entry {:coc/step      :generated
                            :coc/by        "bh.ui-component.atom.bh.table"
                            :coc/version   "no version"
                            :coc/at        (js/Date.)
                            :coc/signature (uuid/make-random-uuid)})
  (spec/valid? :coc/coc default-coc)

  ())


; in Malli
(def coc-step [:enum :generated :updated :deleted])
(def coc-step-map [:map [:coc/step coc-step]])
(def coc-by string?)
(def coc-version string?)
(def coc-at string?)
(def coc-signature string?)
(def coc-entry [:map
                [:coc/step coc-step]
                [:coc/by coc-by]
                [:coc/version coc-version]
                [:coc/at coc-at]
                [:coc/signature coc-signature]])


(comment
  (m/validate coc-step :generated)
  (m/validate coc-step :dummy)

  (def entry {:coc/step      :generated
              :coc/by        "bh.ui-component.atom.bh.table"
              :coc/version   "no version"
              :coc/at        (str (js/Date.))
              :coc/signature (uuid/uuid-string (uuid/make-random-uuid))})


  (m/validate coc-entry entry)
  (m/explain coc-entry entry)


  ; generate some data
  (mg/generate coc-step)
  (mg/generate coc-version)
  (mg/generate coc-entry {:size 64})

  (mg/sample coc-entry {:size 25})

  (gen/sample (mg/generator pos-int?))


  ())
;; endregion

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; Tabular Data examples
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; region
(def tabular-data [{:name "Page A" :uv 4000 :pv 2400 :tv 1500 :amt 2400}
                   {:name "Page B" :uv 3000 :pv 1398 :tv 1500 :amt 2210}
                   {:name "Page C" :uv 2000 :pv 9800 :tv 1500 :amt 2290}
                   {:name "Page D" :uv 2780 :pv 3908 :tv 1500 :amt 2000}
                   {:name "Page E" :uv 1890 :pv 4800 :tv 1500 :amt 2181}
                   {:name "Page F" :uv 2390 :pv 3800 :tv 1500 :amt 2500}
                   {:name "Page G" :uv 3490 :pv 4300 :tv 1500 :amt 2100}])


(def grouped-tabular-data [{:name "Page-A" :values [{:uv 4000 :pv 2400 :amt 400}
                                                    {:uv 1000 :pv 400 :amt 2300}
                                                    {:uv 2000 :pv 3400 :amt 5400}
                                                    {:uv 3000 :pv 2498 :amt 5400}]}])


(def tabular-column-config-data {:brush false
                                 :uv    {:include true :fill "#ff0000" :stroke "#ff0000"
                                         :name    :uv :stackId "" :fillOpacity 0.6}
                                 :pv    {:include true :fill "#00ff00" :stroke "#00ff00"
                                         :name    :pv :stackId "" :fillOpacity 0.6}
                                 :tv    {:include true :fill "#0000ff" :stroke "#0000ff"
                                         :name    :tv :stackId "a" :fillOpacity 0.6}
                                 :amt   {:include true :fill "#ff00ff" :stroke "#ff00ff"
                                         :name    :amt :stackId "a" :fillOpacity 0.6}})


; TODO: convert :color to :fill/:stroke throughout
(def tabular-row-config-data {:Page-A {:name "Page A" :include true :color "#ff0000"} ;"#8884d8"}
                              :Page-B {:name "Page B" :include true :color "#00ff00"} ;"#ffc107"}
                              :Page-C {:name "Page C" :include true :color "#0000ff"} ;"#82ca9d"}
                              :Page-D {:name "Page D" :include true :color "#ffff00"} ;"#ff00ff"}
                              :Page-E {:name "Page E" :include true :color "#ff00ff"} ;"#00e5ff"}
                              :Page-F {:name "Page F" :include true :color "#00ffff"} ;"#4db6ac"}
                              :Page-G {:name "Page G" :include true :color "#888888"} ;"#83a6ed"}
                              :value  {:keys [:uv :pv :tv :amt] :chosen :uv}})


(def tabular-data-org [{:name "Page A" :org "Alpha" :uv 4000 :pv 2400 :amt 2400}
                       {:name "Page B" :org "Bravo" :uv 3000 :pv 1398 :amt 2210}
                       {:name "Page C" :org "Charlie" :uv 2000 :pv 9800 :amt 2290}
                       {:name "Page D" :org "Delta" :uv 2780 :pv 3908 :amt 2000}
                       {:name "Page E" :org "Echo" :uv 1890 :pv 4800 :amt 2181}
                       {:name "Page F" :org "Foxtrot" :uv 2390 :pv 3800 :amt 2500}
                       {:name "Page G" :org "Gamma" :uv 3490 :pv 4300 :amt 2100}])


; in clojure.spec (2)
(spec/def :tabular-data/entry map?)
(spec/def :tabular-data/data (spec/coll-of :tabular-data/entry))


(comment
  (spec/valid? :tabular-data/data tabular-data)
  (spec/valid? :tabular-data/data tabular-data-org)

  ())


; in Malli

;(def tabular-data-entry map?)
(def field-name [:keyword])
(def tabular-data-entry [:map-of field-name any?])
(def tabular-data-data [:sequential tabular-data-entry])

(def example-tabular-data-entry [:map
                                 [:name string?] [:uv number?]
                                 [:pv number?] [:tv number?] [:amt number?]])
(def example-tabular-data-data [:sequential example-tabular-data-entry])

(comment
  (m/validate tabular-data-data tabular-data)
  (m/validate tabular-data-data tabular-data-org)

  (mg/generate tabular-data-data)


  (m/validate example-tabular-data-data tabular-data)
  (mg/generate example-tabular-data-data)

  ())

;; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; Tabular Data with Meta-data examples
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; region

(def meta-tabular-data
  {:metadata {:type   :data/tabular
              :id     :name
              :title  "Tabular Data with Metadata"
              :fields {:name :string :uv :number :pv :number :tv :number :amt :number}}
   :data     tabular-data})

(def BAD-meta-tabular-data-missing-type
  {:metadata {:id     :name
              :title  "Tabular Data with Metadata"
              :fields {:name :string :uv :number :pv :number :tv :number :amt :number}}
   :data     tabular-data})
(def BAD-meta-tabular-data-bad-field
  {:metadata {:type   :data/tabular
              :id     :name
              :title  "Tabular Data with Metadata"
              :fields {:name :keyword}}
   :data     tabular-data})

; in clojure.spec (2)
; TODO: spec needs to include the actual "example" data format so we can generate random values
(spec/def :data/type #{:data/tabular :data/entity})
(spec/def :data/id keyword?)
(spec/def :data/title string?)
(spec/def :data/fields (spec/map-of keyword? #{:string :number}))
(spec/def :data/metadata (spec/keys :req-un [:data/type :data/fields] :opt-un [:data/id :data/title]))
(spec/def :data/data :tabular-data/data)
(spec/def :tabular-data/meta-data (spec/keys :req-un [:data/metadata :data/data]))


(comment
  (spec/valid? :tabular-data/meta-data meta-tabular-data)
  (spec/valid? :tabular-data/meta-data BAD-meta-tabular-data-missing-type)

  (spec/explain :tabular-data/meta-data BAD-meta-tabular-data-missing-type)
  (spec/explain :tabular-data/meta-data BAD-meta-tabular-data-bad-field)

  (expound/expound-str :tabular-data/meta-data meta-tabular-data)
  (expound/expound :tabular-data/meta-data BAD-meta-tabular-data-missing-type)
  (expound/expound-str :tabular-data/meta-data BAD-meta-tabular-data-missing-type)

  (expound/expound :tabular-data/meta-data BAD-meta-tabular-data-bad-field)
  (def error (expound/expound-str :tabular-data/meta-data BAD-meta-tabular-data-bad-field))


  ; we can use re-com/alert-list data structure to display spec failures in place of the
  ; expected UI using the "NEW" alert-list ui-component

  (def alert-msg {:id   0 :alert-type :danger :heading "Parameter Error (Spec Failed)"
                  :body error :padding "8px" :closeable? false})

  ())


; in Malli
(def data-type [:enum :data/tabular :data/entity])
(def data-id keyword?)
(def data-title string?)
(def data-fields [:map-of keyword? [:enum {:error/message "should be: :string OR :number"}
                                    :string :number]])
(def data-metadata [:map
                    [:type data-type] [:fields data-fields]
                    [:id {:optional true} data-id] [:title {:optional true} data-title]])
(def tabular-data-meta-data [:map
                             [:metadata data-metadata]
                             [:data tabular-data-data]])


(def example-data-fields [:map [:name [:enum :string]] [:uv [:enum :number]]
                          [:pv [:enum :number]] [:tv [:enum :number]] [:amt [:enum :number]]])
(def example-data-metadata [:map
                            [:type [:enum :data/tabular]] [:fields example-data-fields]
                            [:id [:enum :name]] [:title {:optional true} data-title]])
(def example-tabular-data-meta-data [:map
                                     [:metadata example-data-metadata]
                                     [:data example-tabular-data-data]])

(def pos-int-limit [:any {:gen/schema [:int {:min 500, :max 8000}]}])
(def example-positive-tabular-data-entry [:map
                                          [:name string?] [:uv pos-int-limit]
                                          [:pv pos-int-limit] [:tv pos-int-limit] [:amt pos-int-limit]])
(def example-positive-tabular-data-data [:sequential example-positive-tabular-data-entry])
(def example-positive-tabular-data-meta-data [:map
                                              [:metadata example-data-metadata]
                                              [:data example-positive-tabular-data-data]])


(comment
  (m/validate tabular-data-meta-data meta-tabular-data)
  (m/validate tabular-data-meta-data BAD-meta-tabular-data-missing-type)
  (m/explain tabular-data-meta-data BAD-meta-tabular-data-missing-type)

  (m/validate tabular-data-meta-data BAD-meta-tabular-data-bad-field)
  (->> BAD-meta-tabular-data-bad-field
    (m/explain tabular-data-meta-data)
    (me/humanize))

  (m/validate example-tabular-data-meta-data meta-tabular-data)

  (mg/generate example-tabular-data-meta-data)

  (mg/generate [:any {:gen/schema [:int {:min 500, :max 8000}]}])

  (mg/generate example-positive-tabular-data-meta-data)


  ())


(defn random-tabular-data []
  (mg/generate example-tabular-data-data))


(defn random-meta-tabular-data []
  (mg/generate example-tabular-data-meta-data))


(defn random-meta-positive-tabular-data []
  (mg/generate example-positive-tabular-data-meta-data))

;; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; Other data structures
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; region

(def some-other-tabular [{:id "Page A" :a 4000 :b 2400 :c 2400}
                         {:id "Page B" :a 3000 :b 1398 :c 2210}
                         {:id "Page C" :a 2000 :b 9800 :c 2290}
                         {:id "Page D" :a 2780 :b 3908 :c 2000}
                         {:id "Page E" :a 1890 :b 4800 :c 2181}
                         {:id "Page F" :a 2390 :b 3800 :c 2500}
                         {:id "Page G" :a 3490 :b 4300 :c 2100}])


(def paired-data [{:name "Group A" :value 400}
                  {:name "Group B" :value 300}
                  {:name "Group C" :value 300}
                  {:name "Group D" :value 200}
                  {:name "Group E" :value 278}
                  {:name "Group F" :value 189}])


(def triplet-data [{:x 100 :y 200 :z 200}
                   {:x 110 :y 280 :z 200}
                   {:x 120 :y 100 :z 260}
                   {:x 140 :y 250 :z 280}
                   {:x 150 :y 400 :z 500}
                   {:x 170 :y 300 :z 400}])


(def hierarchy-data [{:name     "axis"
                      :children [{:name "Axis" :size 24593}
                                 {:name "Axes" :size 1302}
                                 {:name "AxisGridLine" :size 652}
                                 {:name "AxisLabel" :size 636}
                                 {:name "CartesianAxes" :size 6703}]}
                     {:name     "controls"
                      :children [{:name "TooltipControl" :size 8435}
                                 {:name "SelectionControl" :size 7862}
                                 {:name "PanZoomControl" :size 5222}
                                 {:name "HoverControl" :size 4896}
                                 {:name "ControlList" :size 4665}
                                 {:name "ClickControl" :size 3824}
                                 {:name "ExpandControl" :size 2832}
                                 {:name "DragControl" :size 2649}
                                 {:name "AnchorControl" :size 2138}
                                 {:name "Control" :size 1353}
                                 {:name "IControl" :size 763}]}
                     {:name     "data"
                      :children [{:name "Data" :size 20544}
                                 {:name "NodeSprite" :size 19382}
                                 {:name "DataList" :size 19788}
                                 {:name "DataSprite" :size 10349}
                                 {:name "EdgeSprite" :size 3301}
                                 {:name     "render"
                                  :children [{:name "EdgeRenderer" :size 5569}
                                             {:name "ShapeRenderer" :size 2247}
                                             {:name "ArrowType" :size 698}
                                             {:name "IRenderer" :size 353}]}
                                 {:name "ScaleBinding" :size 11275}
                                 {:name "TreeBuilder" :size 9930}
                                 {:name "Tree" :size 7147}]}
                     {:name     "events"
                      :children [{:name "DataEvent" :size 7313}
                                 {:name "SelectionEvent" :size 6880}
                                 {:name "TooltipEvent" :size 3701}
                                 {:name "VisualizationEvent" :size 2117}]}
                     {:name     "legend"
                      :children [{:name "Legend" :size 20859}
                                 {:name "LegendRange" :size 10530}
                                 {:name "LegendItem" :size 4614}]}
                     {:name     "operator"
                      :children [{:name     "distortion"
                                  :children [{:name "Distortion" :size 6314}
                                             {:name "BifocalDistortion" :size 4461}
                                             {:name "FisheyeDistortion" :size 3444}]}
                                 {:name     "encoder"
                                  :children [{:name "PropertyEncoder" :size 4138}
                                             {:name "Encoder" :size 4060}
                                             {:name "ColorEncoder" :size 3179}
                                             {:name "SizeEncoder" :size 1830}
                                             {:name "ShapeEncoder" :size 1690}]}
                                 {:name     "filter"
                                  :children [{:name "FisheyeTreeFilter" :size 5219}
                                             {:name "VisibilityFilter" :size 3509}
                                             {:name "GraphDistanceFilter" :size 3165}]}
                                 {:name "IOperator" :size 1286}
                                 {:name     "label"
                                  :children [{:name "Labeler" :size 9956}
                                             {:name "RadialLabeler" :size 3899}
                                             {:name "StackedAreaLabeler" :size 3202}]}
                                 {:name     "layout"
                                  :children [{:name "RadialTreeLayout" :size 12348}
                                             {:name "NodeLinkTreeLayout" :size 12870}
                                             {:name "CirclePackingLayout" :size 12003}
                                             {:name "CircleLayout" :size 9317}
                                             {:name "TreeMapLayout" :size 9191}
                                             {:name "StackedAreaLayout" :size 9121}
                                             {:name "Layout" :size 7881}
                                             {:name "AxisLayout" :size 6725}
                                             {:name "IcicleTreeLayout" :size 4864}
                                             {:name "DendrogramLayout" :size 4853}
                                             {:name "ForceDirectedLayout" :size 8411}
                                             {:name "BundledEdgeRouter" :size 3727}
                                             {:name "IndentedTreeLayout" :size 3174}
                                             {:name "PieLayout" :size 2728}
                                             {:name "RandomLayout" :size 870}]}
                                 {:name "OperatorList" :size 5248}
                                 {:name "OperatorSequence" :size 4190}
                                 {:name "OperatorSwitch" :size 2581}
                                 {:name "Operator" :size 2490}
                                 {:name "SortOperator" :size 2023}]}])


(def dag-data {:nodes #{{:name :Visit :index 0}
                        {:name :Direct-Favourite :index 1}
                        {:name :Page-Click :index 2}
                        {:name :Detail-Favourite :index 3}
                        {:name :Lost :index 4}}
               :links #{{:source :Visit :target :Direct-Favourite :value 37283}
                        {:source :Visit :target :Page-Click :value 354170}
                        {:source :Page-Click :target :Detail-Favourite :value 62429}
                        {:source :Page-Click :target :Lost :value 291741}}})


; these Malli specs were built (inferred) using "provide"
(def m-dag-data-spec [:map
                      [:nodes [:set [:map [:name keyword?] [:index int?]]]]
                      [:links [:set [:map [:source keyword?]
                                     [:target keyword?] [:value int?]]]]])
(def m-hierarchy-data-spec [:vector
                            [:map
                             [:name string?]
                             [:children
                              [:vector
                               [:map
                                [:name string?]
                                [:size {:optional true} int?]
                                [:children {:optional true} [:vector [:map [:name string?] [:size int?]]]]]]]]])


; create Malli from the data itself!
(comment
  (mp/provide [dag-data dag-data dag-data])

  (m/validate m-dag-data-spec dag-data)


  (m/validate (mp/provide [hierarchy-data]) hierarchy-data)
  (m/validate m-hierarchy-data-spec hierarchy-data)

  ())


(def dag-config-data {:Visit            {:include true :fill "#ff0000" :stroke "#ff0000"}
                      :Direct-Favourite {:include true :fill "#00ff00" :stroke "#00ff00"}
                      :Page-Click       {:include true :fill "#0000ff" :stroke "#0000ff"}
                      :Detail-Favourite {:include true :fill "#12a4a4" :stroke "#12a4a4"}
                      :Lost             {:include true :fill "#ba7b47" :stroke "#ba7b47"}})




;; endregion

