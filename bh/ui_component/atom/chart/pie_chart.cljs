(ns bh.ui-component.atom.chart.pie-chart
  (:require [bh.ui-component.atom.chart.utils :as utils]
            [bh.ui-component.atom.chart.wrapper-2 :as wrapper]
            [bh.ui-component.utils :as ui-utils]
            [bh.ui-component.utils.color :as color]
            [bh.ui-component.utils.example-data :as example-data]
            [re-frame.core :as rf]
            [re-com.core :as rc]
            [taoensso.timbre :as log]
            ["recharts" :refer [ResponsiveContainer PieChart Pie Cell]]))


(log/info "bh.ui-component.atom.chart.pie-chart")


(def source-code '[:> PieChart {:label (utils/override true {} :label)}

                   (utils/non-gridded-chart-components component-id {})

                   [:> Pie {:dataKey           (ui-utils/resolve-sub subscriptions [:value :chosen])
                            :nameKey           (ui-utils/resolve-sub subscriptions [:name :chosen])
                            :data              included
                            :fill              (ui-utils/resolve-sub subscriptions [:fill])
                            :label             (utils/override true {} :label)
                            :isAnimationActive @isAnimationActive?}]])


(def sample-data example-data/meta-tabular-data)
(def sample-config-data {:name  {:keys [:Page-A :Page-B :Page-C :Page-D :Page-E :Page-F :Page-G]}
                         :fill "#888888"
                         :value {:keys [:uv :pv :tv :amt] :chosen :uv}})
(def random-data example-data/random-meta-positive-tabular-data)


(defn local-config [data]

  ;(log/info "local-config" @data)

  (let [d      (get @data :data)
        fields (get-in @data [:metadata :fields])]

    (merge
      {:fill (color/get-color 0)}

      ; process options for :name
      (->> fields
        (filter (fn [[k v]] (= :string v)))
        keys
        ((fn [m]
           {:name {:keys m :chosen (first m)}})))

      (->> d
           (map-indexed (fn [idx entry]
                          ;(log/info "local-config (:color loop)" entry)
                          {(ui-utils/path->keyword (:name entry))
                           {:name  (:name entry)
                            :include true}}))
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


(defn- make-cell-config [component-id data]
    (->> (:data @data)
         (map-indexed (fn [idx {:keys [name] :as item}]
                        [utils/boolean-config component-id name (conj [name] :include)]))
         (into [:<>])))


(defn config-panel [data component-id]
  [rc/v-box :src (rc/at)
   :gap "10px"
   :style {:padding          "15px"
           :border-top       "1px solid #DDD"
           :background-color "#f7f7f7"}
   :children [[utils/non-gridded-chart-config @data component-id]
              [rc/line :src (rc/at) :size "2px"]
              [utils/option component-id ":name" [:name]]
              [rc/line :src (rc/at) :size "2px"]
              [utils/column-picker data component-id ":value" [:value :chosen]]
              [utils/color-config-text component-id ":fill" [:fill] :above-right]
              [rc/v-box :src (rc/at)
               :gap "5px"
               :children [[rc/label :src (rc/at) :label "Pie Slices"]
                          (make-cell-config component-id data)]]]])


(defn- included-cells [data subscriptions]
  (let [ret (->> data
              (filter (fn [{:keys [name]}] (ui-utils/resolve-sub subscriptions [name :include])))
              (into []))]

    ;(log/info "included-cells" data "//" subscriptions "//" ret)

    ret))


(defn- component* [& {:keys [data component-id container-id
                             subscriptions isAnimationActive?]
                      :as   params}]
  (let [d        (if (empty? data) [] (get data :data))
        included (included-cells d subscriptions)]

    [:> ResponsiveContainer
     [:> PieChart {:label (utils/override true {} :label)}

      (utils/non-gridded-chart-components component-id {})

      [:> Pie {:dataKey           (ui-utils/resolve-sub subscriptions [:value :chosen])
               :nameKey           (ui-utils/resolve-sub subscriptions [:name :chosen])
               :data              included
               :fill              (ui-utils/resolve-sub subscriptions [:fill])
               :label             (utils/override true {} :label)
               :isAnimationActive @isAnimationActive?}]]]))


(defn component [& {:keys [component-id] :as params}]

  ;(log/info "component-2" params)

  (let [input-params (assoc params :component* component*
                                   :component-panel wrapper/component-panel
                                   :config config
                                   :local-config local-config)]

    (reduce into [wrapper/base-chart] (seq input-params))))


(def meta-data {:rechart/pie {:component component
                              ;:configurable-component configurable-component
                              :ports     {:data   :port/sink
                                          :config :port/sink}}})


(rf/dispatch-sync [:register-meta meta-data])
