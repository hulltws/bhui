(ns bh.ui-component.atom.chart.radar-chart
  (:require [bh.ui-component.atom.chart.utils :as utils]
            [bh.ui-component.utils.color :as color]
            [bh.ui-component.utils.helpers :as h]
            [bh.ui-component.utils.locals :as l]
            [bh.ui-component.atom.chart.wrapper-2 :as wrapper]
            [bh.ui-component.utils.example-data :as example-data]
            [bh.ui-component.atom.chart.wrapper :as c]
            [bh.ui-component.utils :as ui-utils]
            [re-frame.core :as rf]
            [re-com.core :as rc]
            [reagent.core :as r]
            [taoensso.timbre :as log]

            ["recharts" :refer [ResponsiveContainer RadarChart PolarGrid
                                PolarAngleAxis PolarRadiusAxis Radar]]))


(log/info "bh.ui-component.atom.chart.radar-chart")


(def source-code '[:> RadarChart {:width 400 :height 400 :outerRadius "75%" :data @data}
                   (utils/non-gridded-chart-components component-id)

                   [:> PolarGrid]
                   [:> PolarAngleAxis {:dataKey :subject}]
                   [:> PolarRadiusAxis {:angle "30" :domain [0, 150]}]
                   [:> Radar {:name        "Mark"
                              :dataKey     :A
                              :fill        "#8884d8"
                              :stroke      "#8884d8"
                              :fillOpacity 0.5}]])


(def sample-data example-data/meta-tabular-data)
(def sample-config-data example-data/tabular-column-config-data)
(def random-data example-data/random-meta-positive-tabular-data)


(defn- get-range-across-fields [data]
  (let [source-data (get-in @data [:data])
        all-values  (->> (get-in @data [:metadata :fields])
                      (filter (fn [[k v]] (= :number v)))
                      keys
                      (map-indexed (fn [idx a] (map #(a %) source-data)))
                      (reduce into)
                      (distinct))
        domainMax   (apply max all-values)]
    ;(log/info "domain max = " domainMax)

    {:domain [0 domainMax]}))


(defn- get-field-range [field data]
  (let [source-data (get-in @data [:data])
        domainMin   (reduce min (map #(field %) source-data))
        domainMax   (reduce max (map #(field %) source-data))]
    (if (= domainMin domainMax)
      {:domain [0 domainMax]}
      {:domain [domainMin domainMax]})))


(defn- domain-range [data]
  (let [domainField (get-in @data [:metadata :domain])]
    (if (nil? domainField)
      (get-range-across-fields data)
      (get-field-range domainField data))))


(defn local-config
  "provides both the definition and the initial default values for various properties that
  allow user to customize the visualization of the chart.

  ---

   - data : (atom) atom containing the data and metadata for this chart

> See Also:
>
> [Recharts/line-chart](https://recharts.org/en-US/api/LineChart)
> [tabular-data]()
  "
  [data]
  ;(log/info "local-config" data)
  (let [ret (merge (domain-range data)
              (->> (get-in @data [:metadata :fields])
                (filter (fn [[k v]] (= :number v)))
                keys
                (map-indexed (fn [idx a]
                               {a {:include     true
                                   :name        a
                                   :fill        (color/get-color idx)
                                   :stroke      (color/get-color idx)
                                   :fillOpacity 0.6}}))
                (into {})))]
    ;(log/info "local-config" ret)
    ret))


(defn config
  "constructs the configuration panel for the chart's configurable properties. This is specific to
  this being a radar-chart component (see [[local-config]]).

  Merges together the configuration needed for:

  1. radar charts
  2. pub/sub between components of a container
  3. `default-config` for all Rechart-based types
  4. the `tab-panel` for view/edit configuration properties and data
  5. sets properties of the default-config (local config properties are just set inside [[local-config]])
  6. sets meta-data for properties this component publishes (`:pub`) or subscribes (`:sub`)

  ---

  - component-id : (string) unique id of the chart
  - data : (atom) metadata wrapped data  to display
  "
  [component-id data]
  ;(log/info "configgg : " @data)
  (-> ui-utils/default-pub-sub
    (merge
      utils/default-config
      {:tab-panel {:value     (keyword component-id "config")
                   :data-path [:containers (keyword component-id) :tab-panel]}}

      (local-config data))
    (assoc-in [:fullMark :include] false)))


(defn- radar-config [component-id label path position]
  ;(log/info "radar-config" component-id label path position)
  [rc/v-box :src (rc/at)
   :gap "5px"
   :children [[utils/boolean-config component-id label (conj path :include)]
              [utils/color-config component-id ":fill" (conj path :fill) position]
              [utils/color-config component-id ":stroke" (conj path :stroke) position]
              [utils/slider-config component-id 0 1 0.1 (conj path :fillOpacity)]]])


(defn- make-radar-config [component-id data]
  ;(log/info "make-radar-config" @data)
  (let [ret (->> (get-in @data [:metadata :fields])
              (filter (fn [[k v]] (= :number v)))
              keys
              (map-indexed (fn [idx a]
                             ^{:key (str "radar-config-" a)}
                             [radar-config component-id a [a] :below-right]))
              (into []))]

    ;(log/info "make-radar-config" ret)
    ret))


(defn config-panel
  "the panel of configuration controls

  ---

  - data : (atom) data to display (may be used by the standard configuration components for thins like axes, etc.\n  - config : (atom) holds all the configuration settings made by the user
  "
  [data component-id]
  ;(log/info "radar config panel")

  [rc/v-box :src (rc/at)
   :gap "10px"
   :width "100%"
   :height "100%"
   :style {:padding          "15px"
           :border-top       "1px solid #DDD"
           :background-color "#f7f7f7"}
   :children [;[utils/non-gridded-chart-config component-id]
              ;[rc/line :src (rc/at) :size "2px"]
              [rc/h-box :src (rc/at)
               :width "100%"
               :height "100%"
               :style ui-utils/h-wrap
               :gap "10px"
               :children (make-radar-config component-id data)]]])


(defn- make-radar-display [data subscriptions isAnimationActive?]
  ;(log/info "make-radar-display" data "//" subscriptions)
  (let [ret (->> (get-in data [:metadata :fields])
              (filter (fn [[_ v]] (= :number v)))
              keys
              (map (fn [a]
                     (if (ui-utils/resolve-sub subscriptions [a :include])
                       [:> Radar {:name        (ui-utils/resolve-sub subscriptions [a :name])
                                  :dataKey     a
                                  :fill        (ui-utils/resolve-sub subscriptions [a :fill])
                                  :stroke      (ui-utils/resolve-sub subscriptions [a :stroke])
                                  :fillOpacity (ui-utils/resolve-sub subscriptions [a :fillOpacity])}]
                       [])))
              (remove empty?)
              (into [:<>]))]
    ;(log/info "make-radar-display (ret)" ret)
    ret))


(defn- component*
  "the chart to draw, taking cues from the settings of the configuration panel

  ---

  - data : (atom) any data used by the component's ui
  - component-id : (string) unique identifier for this specific widget
  "
  [& {:keys [data component-id container-id
             subscriptions isAnimationActive?]
      :as params}]

  ;(log/info "radar component*" data " // " subscriptions)

  (let [d (if (empty? data) [] (get data :data))
        domain (ui-utils/resolve-sub subscriptions [:domain])]

    [:> ResponsiveContainer
     [:> RadarChart {:data d}
      [:> PolarGrid]
      [:> PolarAngleAxis {:dataKey :name}]
      [:> PolarRadiusAxis {:angle 60 :domain (or domain [0 10000])}]

      ;(utils/non-gridded-chart-components component-id {})

      (make-radar-display data subscriptions isAnimationActive?)]]))


(defn component [& {:keys [component-id] :as params}]

  ;(log/info "Radar component-2" params)

  (let [input-params (assoc params :component* component*
                                   :component-panel wrapper/component-panel
                                   :config config
                                   :local-config local-config)]

    (reduce into [wrapper/base-chart] (seq input-params))))


(def meta-data {:rechart/radar {:component component
                                ;:configurable-component configurable-component
                                :ports     {:data   :port/sink
                                            :config :port/sink}}})


(rf/dispatch-sync [:register-meta meta-data])






; explore the data fields
(comment
  (def domainField :fullMark)
  (def source-data (get-in sample-data [:data]))
  (reduce max (map #(domainField %) source-data))
  (let [source-data (get-in @sample-data [:data])])

  (def fields (get-in @sample-data [:metadata :fields]))
  (filter (fn [[k v]] (= :number v)) fields)
  keys


  (def numValues nil)
  (concat numValues '(1 2 3))
  (concat numValues '(3 4 5))

  (def source-data (get-in @sample-data [:data]))
  (->> (get-in @sample-data [:metadata :fields])
    (filter (fn [[k v]] (= :number v)))
    keys
    (map-indexed (fn [idx a] (map #(a %) source-data)))
    (reduce into)
    (distinct))

  (def fieldNames (get-in @sample-data [:metadata :fields]))
  (def numFieldsOnly (filter (fn [[k v]] (= :number v)) fieldNames))
  (def keysOnly (keys numFieldsOnly))
  (def res (map-indexed (fn [idx a] (a source-data)) keysOnly))
  ())


; compute the range for the domain (scale of the axis)
(comment
  (def data sample-data)
  (def source-data (get-in @data [:data]))
  (def all-values (->> (get-in @data [:metadata :fields])
                    (filter (fn [[k v]] (= :number v)))
                    keys
                    (map-indexed (fn [idx a] (map #(a %) source-data)))
                    (reduce into)
                    (distinct)))
  (def domainMin (apply min all-values))
  (def domainMax (apply max all-values))

  (->> (get-in @data [:metadata :fields])
    (filter (fn [[k v]] (= :number v)))
    keys
    (map-indexed (fn [idx a] (map #(a %) source-data)))
    (reduce into)
    (distinct))
  ())


; defs for repl testing
(comment
  (def component-id "radar-chart-demo/radar-chart")
  (def data sample-data)
  (def subscriptions (ui-utils/build-subs component-id (local-config data)))
  ())


