(ns bh.ui-component.atom.chart.radial-bar-chart
  (:require [bh.ui-component.atom.chart.utils :as utils]
            [bh.ui-component.atom.chart.wrapper-2 :as wrapper]
            [bh.ui-component.utils :as ui-utils]
            [bh.ui-component.utils.color :as color]
            [bh.ui-component.utils.example-data :as example-data]
            [re-frame.core :as rf]
            [re-com.core :as rc]
            [reagent.core :as r]
            [taoensso.timbre :as log]
            ["recharts" :refer [ResponsiveContainer RadialBarChart RadialBar Legend Tooltip Cell]]))


(log/info "bh.ui-component.atom.chart.radial-bar-chart")


(def source-code '[:> RadialBarChart {:innerRadius "10%"
                                      :outerRadius "80%"
                                      :data        included
                                      :startAngle  180
                                      :endAngle    0}

                   [:> RadialBar {:minAngle   15
                                  :background {:clockWise true}
                                  :dataKey    (ui-utils/resolve-sub subscriptions [:value :chosen])}
                    (make-radial-bar-display included subscriptions isAnimationActive?)]

                   [:> Legend]
                   [:> Tooltip {:content custom-tooltip}]])


(def sample-data example-data/meta-tabular-data)
(def sample-config-data example-data/tabular-row-config-data)
(def random-data example-data/random-meta-positive-tabular-data)


(defn local-config [data]
  (let [d      (get @data :data)
        fields (get-in @data [:metadata :fields])]

    (merge
      ; process options for :name
      (->> fields
        (filter (fn [[k v]] (= :string v)))
        keys
        ((fn [m]
           {:name {:keys m :chosen (first m)}})))

      ; process :name to map up the :colors
      (->> d
        (map-indexed (fn [idx entry]
                       {(ui-utils/path->keyword (:name entry))
                        {:name    (:name entry)
                         :include true
                         :color   (nth (cycle color/default-stroke-fill-colors) idx)}}))
        (into {}))

      ; process options for :value
      (->> fields
        (filter (fn [[k v]] (= :number v)))
        keys
        ((fn [m]
           {:value {:keys m :chosen (first m)}}))))))


(defn config
  "constructs the configuration data structure for the widget. This is specific to this being a radar-chart component.

  ---

  - component-id : (string) id of the widget, in this specific case
  "
  [component-id data]
  (merge
    ui-utils/default-pub-sub
    utils/default-config
    (ui-utils/config-tab-panel component-id)
    (local-config data)))


(defn- radial-config [component-id label path position]
  (let [p (ui-utils/path->keyword path)]
    [rc/h-box :src (rc/at)
     :gap "5px"
     :children [[utils/boolean-config component-id "" (conj [p] :include)]
                [utils/color-config-text component-id label (conj [p] :color) position]]]))


(defn- make-radial-bar-config [component-id data]
  (->> (:data @data)
    (map-indexed (fn [idx {:keys [name] :as item}]
                   [radial-config component-id name [name] :above-right]))
    (into [:<>])))


(defn config-panel
  "the panel of configuration controls

  ---

  - data : (atom) data to display (may be used by the standard configuration components for thins like axes, etc.\n  - config : (atom) holds all the configuration settings made by the user
  "
  [data component-id]

  [rc/v-box :src (rc/at)
   :gap "10px"
   :width "100%"
   :style {:padding          "15px"
           :border-top       "1px solid #DDD"
           :background-color "#f7f7f7"}
   :children [[utils/non-gridded-chart-components component-id]
              [rc/line :src (rc/at) :size "2px"]
              [utils/option component-id ":name" [:name]]
              [rc/line :src (rc/at) :size "2px"]
              [utils/column-picker data component-id ":value" [:value :chosen]]
              [rc/v-box :src (rc/at)
               :gap "10px"
               :children [[rc/label :src (rc/at) :label "Bar Colors"]
                          (make-radial-bar-config component-id data)]]]])


(defn- make-cell [name idx subscriptions]
  [:> Cell {:key  (str name) ;(ui-utils/resolve-sub subscriptions [:value :chosen]) "-" idx)
            :fill (or (ui-utils/resolve-sub subscriptions [name :color])
                    (color/get-color 0))}])


(defn- make-radial-bar-display [data subscriptions isAnimationActive?]
  ;(log/info "make-radial-bar-display: " data "//" subscriptions)

  (let [ret (->> data
              (map-indexed (fn [idx {:keys [name]}]
                             ;(log/info "make-radial-bar-display (name)" name)
                             (if (ui-utils/resolve-sub subscriptions [name :include])
                               (make-cell name idx subscriptions)
                               [])))
              (remove empty?)
              (into [:<>]))]
    ;(log/info "make-radial-bar-display (ret)" ret)

    ret))


(defn- included-cells [data subscriptions]
  (->> data
    (filter (fn [{:keys [name]}] (ui-utils/resolve-sub subscriptions [name :include])))
    (into [])))


(defn- custom-tooltip [tooltip-map]
  ;(log/info "custom-tooltip" (js->clj x))
  (let [{:keys [payload]} (js->clj tooltip-map :keywordize-keys true)
        [p _] payload
        p-p (:payload p)
        dataKey (:dataKey p)
        name (:name p-p)
        data (get p-p (keyword dataKey))]

    (r/as-element
      [rc/v-box
       :style {:background "rgba(255, 255, 255, 0.8)"
               :border     "1px solid" :border-radius "3px"
               :box-shadow "5px 5px 5px 2px"
               :margin     "5px" :padding "5px"}
       :gap "2px"
       :children [[:p.has-text-centered.has-text-weight-bold (str name)]
                  [rc/line :size "1px"]
                  [:p.has-text-centered (str dataKey " : " data)]]])))


(defn- component* [& {:keys [data component-id container-id
                             subscriptions isAnimationActive?]
                      :as   params}]
  (let [d        (if (empty? data) [] (get data :data))
        included (included-cells d subscriptions)]

    ;(log/info "radial component* data: " d "//" included)

    [:> ResponsiveContainer
     [:> RadialBarChart {:innerRadius "10%"
                         :outerRadius "80%"
                         :data        included
                         :startAngle  180
                         :endAngle    0}

      [:> RadialBar {:minAngle   15
                     :background {:clockWise true}
                     :dataKey    (ui-utils/resolve-sub subscriptions [:value :chosen])}
       (make-radial-bar-display included subscriptions isAnimationActive?)]

      [:> Legend] ;{:iconSize 10 :width 120 :height 140 :layout "horizontal" :verticalAlign "bottom" :align "middle"}]
      [:> Tooltip {:content custom-tooltip}]]]))


(defn component [& {:keys [component-id] :as params}]

  (let [input-params (assoc params :component* component*
                                   :component-panel wrapper/component-panel
                                   :config config
                                   :local-config local-config)]

    (reduce into [wrapper/base-chart] (seq input-params))))


(def meta-data {:rechart/radial-bar {:component component
                                     :ports     {:data   :port/sink
                                                 :config :port/sink}}})


(rf/dispatch-sync [:register-meta meta-data])






(comment

  (def data example-data/meta-tabular-data)

  (:data data)
  (->> data
    :data
    (map (juxt :name :uv))
    (map (fn [[k v]] {:name k :uv v})))
  (config "comp-id" data)


  ;{[:tv :fill] #object[reagent.ratom.Reaction {:val "#82ca9d"}],
  ; [:uv :fill] #object[reagent.ratom.Reaction {:val "#8884d8"}],
  ; [:pv] #object[reagent.ratom.Reaction {:val {:include false, :fill "#ffc107", :stackId ""}}],
  ; [:pv :stackId] #object[reagent.ratom.Reaction {:val ""}],
  ; [:tv] #object[reagent.ratom.Reaction {:val {:include false, :fill "#82ca9d", :stackId ""}}],
  ; [:amt :stackId] #object[reagent.ratom.Reaction {:val ""}],
  ; [:pv :include] #object[reagent.ratom.Reaction {:val nil}],
  ; [:amt :fill] #object[reagent.ratom.Reaction {:val "#ff00ff"}],
  ; [:uv :include] #object[reagent.ratom.Reaction {:val true}],
  ; [:brush] #object[reagent.ratom.Reaction {:val nil}],
  ; [:tv :include] #object[reagent.ratom.Reaction {:val nil}],
  ; [:amt] #object[reagent.ratom.Reaction {:val {:include false, :fill "#ff00ff", :stackId ""}}],
  ; [:pv :fill] #object[reagent.ratom.Reaction {:val "#ffc107"}],
  ; [:uv :stackId] #object[reagent.ratom.Reaction {:val ""}],
  ; [:tv :stackId] #object[reagent.ratom.Reaction {:val ""}],
  ; [:uv] #object[reagent.ratom.Reaction {:val {:include true, :fill "#8884d8", :stackId ""}}],
  ; [:amt :include] #object[reagent.ratom.Reaction {:val nil}]}


  ())

