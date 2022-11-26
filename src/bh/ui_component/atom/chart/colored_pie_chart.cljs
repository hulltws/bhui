(ns bh.ui-component.atom.chart.colored-pie-chart
  (:require [bh.ui-component.atom.chart.utils :as utils]
            [bh.ui-component.atom.chart.wrapper-2 :as wrapper]
            [bh.ui-component.utils :as ui-utils]
            [bh.ui-component.utils.color :as color]
            [bh.ui-component.utils.example-data :as example-data]
            [re-com.core :as rc]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [taoensso.timbre :as log]
            ["recharts" :refer [ResponsiveContainer PieChart Pie Cell Tooltip Legend]]))


(log/info "bh.ui-component.atom.chart.colored-pie-chart")


(def source-code '[:> PieChart {:label true} (utils/override true {} :label)
                   [:> Pie {:dataKey           (ui-utils/resolve-sub subscriptions [:value :chosen])
                            :nameKey           (ui-utils/resolve-sub subscriptions [:name :chosen])
                            :data              included
                            :label             (utils/override true {} :label)
                            :isAnimationActive @isAnimationActive?}
                    (make-cells d subscriptions)]
                   [:> Legend]
                   [:> Tooltip {:content custom-tooltip}]])


(def sample-data example-data/meta-tabular-data)
(def sample-config-data example-data/tabular-row-config-data)
(def random-data example-data/random-meta-positive-tabular-data)


(defn local-config [data]

  ;(log/info "local-config" @data)

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
                       ;(log/info "local-config (:color loop)" entry)
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


(defn config [component-id data]
  (merge
    ui-utils/default-pub-sub
    utils/default-config
    (ui-utils/config-tab-panel component-id)
    (local-config data)))


(defn- cell-config [component-id label path position]
  (let [p (ui-utils/path->keyword path)]
    ;(log/info "cell-config" component-id "//" label "//" p)
    [rc/h-box
     :gap "5px"
     :children [[utils/boolean-config component-id "" (conj [p] :include)]
                [utils/color-config-text component-id label (conj [p] :color) :right-above]]]))


(defn- make-cell-config [component-id data]
  (->> (:data @data)
    (map-indexed (fn [idx {:keys [name] :as item}]
                   [cell-config component-id name [name] :above-right]))
    (into [:<>])))


(defn config-panel [data component-id]
  ;(log/info "config-panel" component-id "//" @data)

  [rc/v-box :src (rc/at)
   :gap "10px"
   :width "400px"
   :style {:padding          "15px"
           :border-top       "1px solid #DDD"
           :background-color "#f7f7f7"}
   :children [[utils/non-gridded-chart-config @data component-id]
              [rc/line :src (rc/at) :size "2px"]
              [utils/option component-id ":name" [:name]]
              [rc/line :src (rc/at) :size "2px"]
              [utils/column-picker data component-id ":value" [:value :chosen]]
              [rc/v-box :src (rc/at)
               :gap "5px"
               :children [[rc/label :src (rc/at) :label "Pie Colors"]
                          (make-cell-config component-id data)]]]])


(defn- make-cells [data subscriptions]
  ;(log/info "make-cells" data
  ;  "// (subscriptions)" subscriptions)

  (let [ret (->> data
              (map-indexed (fn [idx {:keys [name]}]
                             (if (ui-utils/resolve-sub subscriptions [name :include])
                               (do
                                 [:> Cell {:key  (str "cell-" idx)
                                           :fill (or (ui-utils/resolve-sub subscriptions [name :color])
                                                   (color/get-color 0))}])
                               [])))
              (remove empty?)
              (into [:<>]))]
    ;(log/info "ret" ret)

    ret))


(defn- included-cells [data subscriptions]
  (->> data
    (filter (fn [{:keys [name]}] (ui-utils/resolve-sub subscriptions [name :include])))
    (into [])))


(defn- custom-tooltip [tooltip-map]
  ;(log/info "custom-tooltip" (js->clj x))
  (let [{:keys [payload]} (js->clj tooltip-map :keywordize-keys true)
        [p _] payload
        p-p     (:payload p)
        dataKey (:dataKey p)
        name    (:name p-p)
        data    (get p-p (keyword dataKey))]

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

    ;(log/info "colored-pie-chart" component-id
    ;"//" data "//" d
    ;"//" included

    [:> ResponsiveContainer
     [:> PieChart {:label true} (utils/override true {} :label)

      [:> Pie {:dataKey           (ui-utils/resolve-sub subscriptions [:value :chosen])
               :nameKey           (ui-utils/resolve-sub subscriptions [:name :chosen])
               :data              included
               :label             (utils/override true {} :label)
               :isAnimationActive @isAnimationActive?}
       (make-cells d subscriptions)]
      [:> Legend]                                           ;{:iconSize 10 :width 120 :height 140 :layout "horizontal" :verticalAlign "bottom" :align "middle"}]
      [:> Tooltip {:content custom-tooltip}]]]))


(defn component [& {:keys [component-id] :as params}]

  ;(log/info "component-2" params)

  (let [input-params (assoc params :component* component*
                                   :component-panel wrapper/component-panel
                                   :config config
                                   :local-config local-config)]

    (reduce into [wrapper/base-chart] (seq input-params))))


(def meta-data {:rechart/colored-pie {:component component
                                      ;:configurable-component configurable-component
                                      :ports     {:data   :port/sink
                                                  :config :port/sink}}})


(rf/dispatch-sync [:register-meta meta-data])
