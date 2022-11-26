(ns bh.ui-component.atom.chart.funnel-chart
  (:require [bh.ui-component.atom.chart.utils :as utils]
            [bh.ui-component.atom.chart.wrapper-2 :as wrapper]
            [bh.ui-component.utils :as ui-utils]
            [bh.ui-component.utils.color :as color]
            [bh.ui-component.utils.example-data :as example-data]
            [re-com.core :as rc]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            ["recharts" :refer [ResponsiveContainer FunnelChart Funnel Cell LabelList
                                XAxis YAxis CartesianGrid Tooltip Brush]]))


(log/info "bh.ui-component.atom.chart.funnel-chart")


(def sample-data example-data/meta-tabular-data)
(def sample-config-data example-data/tabular-row-config-data)
(def random-data example-data/random-meta-positive-tabular-data)


(defn local-config [data]
  ;(log/info "local-config" data)
  (let [d      (get @data :data)
        fields (get-in @data [:metadata :fields])]
    ;(log/info "configgg : " @data)
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


(defn config [component-id data]
  ;(log/info "config" @data)
  (merge
    ui-utils/default-pub-sub
    utils/default-config
    (ui-utils/config-tab-panel component-id)
    (local-config data)))


(defn- color-anchors [component-id]
  [:<>
   (doall
     (map (fn [[id color-data]]
            (let [text (:name color-data)]
              ^{:key id} [utils/color-config-text component-id text [id :color] :right-above]))
       @(ui-utils/subscribe-local component-id [:colors])))])


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
  ;(log/info "config-panel")
  [rc/v-box :src (rc/at)
   :gap "10px"
   :width "100%"
   :style {:padding          "15px"
           :border-top       "1px solid #DDD"
           :background-color "#f7f7f7"}
   :children [[utils/non-gridded-chart-config component-id]
              [rc/line :src (rc/at) :size "2px"]
              [utils/option component-id ":name" [:name]]
              [rc/line :src (rc/at) :size "2px"]
              [utils/column-picker data component-id ":value" [:value :chosen]]
              [rc/v-box :src (rc/at)
               :gap "5px"
               :children [[rc/label :src (rc/at) :label "Funnel Colors"]
                          (make-cell-config component-id data)]]]])


(defn make-cells [data subscriptions]
  (let [ret (->> data
              (map-indexed
                (fn [idx {name :name}]
                  (if (ui-utils/resolve-sub subscriptions [name :include])
                    ^{:key (str idx name)}
                    [:> Cell {:key  (str "cell-" idx)
                              :fill (or (ui-utils/resolve-sub subscriptions [name :color])
                                      (color/get-color 0))}]
                    [])))
              (remove empty?)
              (into [:<>]))]
    ret))


(defn- included-cells [data subscriptions]
  (->> data
    (filter (fn [{:keys [name]}] (ui-utils/resolve-sub subscriptions [name :include])))
    (into [])))


(defn- component* [& {:keys [data component-id container-id
                             subscriptions isAnimationActive?]
                      :as   params}]

  ;(log/info "funnel component*" params)
  (let [d        (if (empty? data) [] (get data :data))
        included (included-cells d subscriptions)]

    [:> ResponsiveContainer
     [:> FunnelChart {:label true}
      ;(utils/override true {} :label)

      ;(utils/non-gridded-chart-components component-id {})

      [:> Funnel {:dataKey           (ui-utils/resolve-sub subscriptions [:value :chosen])
                  :nameKey           (ui-utils/resolve-sub subscriptions [:name :chosen])
                  :label             true
                  :data              included
                  :isAnimationActive @isAnimationActive?}
       (make-cells d subscriptions)
       [:> LabelList {:position :right :fill "#000000" :stroke "none" :dataKey (ui-utils/resolve-sub subscriptions [:value :chosen])}]]]]))


(def source-code `[:> FunnelChart {:height 400 :width 500}
                   [:> Funnel {:dataKey           :value
                               :nameKey           "name"
                               :label             true
                               :data              @data
                               :isAnimationActive @isAnimationActive?}]])


(defn component [& {:keys [component-id] :as params}]

  ;(log/info "component-2 funnel" params)

  (let [input-params (assoc params :component* component*
                                   :component-panel wrapper/component-panel
                                   :config config
                                   :local-config local-config)]

    (reduce into [wrapper/base-chart] (seq input-params))))


(def meta-data {:rechart/funnel {:component component
                                 ;:configurable-component configurable-component
                                 :ports     {:data   :port/sink
                                             :config :port/sink}}})


(rf/dispatch-sync [:register-meta meta-data])


(comment

  ())










