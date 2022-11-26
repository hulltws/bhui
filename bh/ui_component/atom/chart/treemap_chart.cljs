(ns bh.ui-component.atom.chart.treemap-chart
  (:require [bh.ui-component.atom.chart.utils :as utils]
            [bh.ui-component.utils.example-data :as data]
            [bh.ui-component.atom.chart.wrapper :as c]
            [bh.ui-component.utils :as ui-utils]
            [re-frame.core :as rf]
            [re-com.core :as rc]
            [reagent.core :as r]
            ["recharts" :refer [ResponsiveContainer Treemap]]))


(def sample-data
  "the Treemap Chart works best with \"hierarchical data\" so we return the hierarchy-data from utils"
  (r/atom data/hierarchy-data))


(def default-ratio (/ 4 3))
(def default-stroke "#ffffff")
(def default-fill "#8884d8")


(defn config
  "constructs the configuration panel for the chart's configurable properties. This is specific to
  this being a line-chart component (see [[local-config]]).

  Merges together the configuration needed for:

  1. line charts
  2. pub/sub between components of a container
  3. `default-config` for all Rechart-based types
  4. the `tab-panel` for view/edit configuration properties and data
  5. sets properties of the default-config (local config properties are just set inside [[local-config]])
  6. sets meta-data for properties this component publishes (`:pub`) or subscribes (`:sub`)

  ---

  - component-id : (string) unique id of the chart
  "
  [component-id data]
  (merge
    ui-utils/default-pub-sub
    {:tab-panel         {:value     (keyword component-id "config")
                         :data-path [:containers (keyword component-id) :tab-panel]}
     :isAnimationActive true
     :ratio             {:include true
                         :n       4
                         :d       3}
     :stroke            {:color "#ffffff"}
     :fill              {:color "#8884d8"}}))


(defn- ratio-config
  "builds the ui components needed to configure the `ration` property of the treemap

> See also:
>
> [Recharts/treemap](https://recharts.org/en-US/api/Treemap)
"
  [component-id]
  [rc/h-box :src (rc/at)
   :gap "5px"
   :children [[utils/boolean-config component-id ":ratio" [:ratio :include]]
              [rc/v-box :src (rc/at)
               :gap "5px"
               :children [[utils/slider-config component-id 1 10 [:ratio :n]]
                          [utils/slider-config component-id 1 10 [:ratio :d]]]]]])


(defn- config-panel
  "the panel of configuration controls

  ---

  - data : (atom) data to display (may be used by the standard configuration components for thins like axes, etc.
  - component-id : (string) unique identifier for this chart instance
  "
  [_ component-id]

  [rc/v-box :src (rc/at)
   :gap "10px"
   :width "400px"
   :height "500px"
   :style {:padding          "15px"
           :border-top       "1px solid #DDD"
           :background-color "#f7f7f7"}
   :children [[utils/isAnimationActive component-id]
              [rc/line :src (rc/at) :size "2px"]
              [rc/v-box :src (rc/at)
               :gap "10px"
               :children [[ratio-config component-id]
                          [utils/color-config-text component-id ":stroke" [:stroke :color]]
                          [utils/color-config-text component-id ":fill" [:fill :color]]]]]])


(def source-code `[:> Treemap
                   {:width             400 :height 400
                    :data              @data
                    :dataKey           "size"
                    :isAnimationActive true
                    :ratio             (/ 4 3)
                    :stroke            "#ffffff"
                    :fill              "#8884d8"}])


(defn- component-panel
  "the chart to draw, taking cues from the settings of the configuration panel

  ---

  - data : (atom) any data shown by the component's ui
  - component-id : (string) unique identifier for this widget instance
  "
  [data component-id container-id ui]
  (let [;ratio (ui-utils/subscribe-local component-id [:ratio :include])
        isAnimationActive? (ui-utils/subscribe-local component-id [:isAnimationActive])
        stroke             (ui-utils/subscribe-local component-id [:stroke :color])
        fill               (ui-utils/subscribe-local component-id [:fill :color])]

    (fn [data component-id container-id ui]
      [:> ResponsiveContainer
       [:> Treemap
        {:data              @data
         :dataKey           "size"
         :isAnimationActive @isAnimationActive?
         :ratio             default-ratio
         :stroke            @stroke
         :fill              @fill}]])))


(defn configurable-component
  "the chart to draw, taking cues from the settings of the configuration panel

  ---

  - :data : (atom) any data shown by the component's ui
  - :component-id : (string) unique identifier for this chart instance within this container
  - :container-id : (string) name of the container this chart is inside of
  "
  [& {:keys [data component-id container-id ui]}]
  [c/base-chart
   :data data
   :config (config component-id data)
   :component-id component-id
   :container-id (or container-id "")
   :data-panel utils/hierarchy-data-panel
   :config-panel config-panel
   :component-panel component-panel
   :ui ui])


(defn component
  "the chart to draw. this variant does NOT provide a configuration panel

  ---

  - :data : (atom) any data shown by the component's ui
  - :component-id : (string) unique identifier for this chart instance within this container
  - :container-id : (string) name of the container this chart is inside of
  "
  [& {:keys [data component-id container-id ui]}]
  [c/base-chart
   :data data
   :config (config component-id data)
   :component-id component-id
   :container-id (or container-id "")
   :component-panel component-panel
   :ui ui])


(def meta-data {:rechart/treemap {:component component
                                  ;:configurable-component configurable-component
                                  :ports     {:data   :port/sink
                                              :config :port/sink}}})


(rf/dispatch-sync [:register-meta meta-data])




(comment
  (def component-id "treemap-chart-demo")

  ())