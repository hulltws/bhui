(ns bh.ui-component.atom.oz.line-chart
  (:require [bh.ui-component.atom.chart.utils :as utils]
            [bh.ui-component.atom.chart.wrapper :as c]
            [bh.ui-component.utils :as ui-utils]

            [oz.core :as oz]
            [re-com.core :as rc]

            [reagent.core :as r]
            [taoensso.timbre :as log]))



(def source-code '[oz.core/vega-lite
                   {:data     {:values (get @data :data)}
                    :mark     "bar"
                    :encoding {:x     {:field "time"
                                       :type  "ordinal"}
                               :y     {:aggregate "sum"
                                       :field     "quantity"
                                       :type      "quantitative"}
                               :color {:field "item"
                                       :type  "nominal"}}}])


(defn- config [component-id data]
  (-> ui-utils/default-pub-sub
    (merge
      utils/default-config
      {:type "oz-line-chart"}
      (ui-utils/config-tab-panel component-id))))


(defn- config-panel
  [data component-id]

  [:div "config panel here"])


(def sample-data
  (r/atom {:$schema "https://vega.github.io/schema/vega/v5.json"
           :width   400
           :height  400
           :padding 5

           :signals [{:name  "interpolate"
                      :value "linear"
                      :bind  {:input   "select"
                              :options ["basis"
                                        "cardinal"
                                        "catmull-rom"
                                        "linear"
                                        "monotone"
                                        "natural"
                                        "step"
                                        "step-after"
                                        "step-before"]}}]

           :data    [{:name   "table"
                      :values [{:x 0 :y 28 :c 0} {:x 0 :y 20 :c 1}
                               {:x 1 :y 43 :c 0} {:x 1 :y 35 :c 1}
                               {:x 2 :y 81 :c 0} {:x 2 :y 10 :c 1}
                               {:x 3 :y 19 :c 0} {:x 3 :y 15 :c 1}
                               {:x 4 :y 52 :c 0} {:x 4 :y 48 :c 1}
                               {:x 5 :y 24 :c 0} {:x 5 :y 28 :c 1}
                               {:x 6 :y 87 :c 0} {:x 6 :y 66 :c 1}
                               {:x 7 :y 17 :c 0} {:x 7 :y 27 :c 1}
                               {:x 8 :y 68 :c 0} {:x 8 :y 16 :c 1}
                               {:x 9 :y 49 :c 0} {:x 9 :y 25 :c 1}]}]

           :scales  [{:name   :x-axis
                      :type   :point
                      :domain {:data "table" :field :x}
                      :range  :width
                      :round  true}
                     {:name   :y-axis
                      :type   :linear
                      :domain {:data "table" :field :y}
                      :nice   true
                      :zero   true
                      :range  :height}
                     {:name   :color
                      :type   :ordinal
                      :domain {:data "table" :field :c}
                      :range  :category}]

           :axes    [{:orient :bottom :scale :x-axis}
                     {:orient :left :scale :y-axis}]

           :marks   [{:type "group"
                      :from {:facet {:name    "series"
                                     :data    "table"
                                     :groupby :c}}
                      :marks [{:type   "line"
                               :from   {:data "series"}
                               :encode {:enter  {:x           {:scale :x-axis :field :x}
                                                 :y           {:scale :y-axis :field :y}
                                                 :stroke      {:scale :color :field :c}
                                                 :strokeWidth {:value 2}}
                                        :update {:interpolate   {:signal "interpolate"}
                                                 :strokeOpacity {:value 1}}
                                        :hover  {:strokeOpacity {:value 0.5}}}}]}]}))


(defn- component-panel
  [data component-id]

  [:div {:style {:width "400px" :height "500px"}}
   [oz/vega-lite @data]])



(defn component
  ([& {:keys [data component-id container-id]}]

   ;(log/info "line-chart" @data)

   ;(if (not= :tabular (get-in @data [:metadata :type]))
   ;  [rc/alert-box :src (rc/at)
   ;   :id (str container-id "/" component-id ".ERROR")
   ;   :alert-type :danger
   ;   :closeable? false
   ;   :body [:div "The data passed is NOT of type :tabular!"]]

   (let [id (r/atom nil)]

     (fn []
       (when (nil? @id)
         (reset! id component-id)
         (ui-utils/init-container-locals @id (config @id data))
         (ui-utils/dispatch-local @id [:container] (or container-id "")))

       ;(log/info "component" @id)

       [c/configurable-chart
        :data data
        :id @id
        :data-panel utils/dummy-data-panel
        :config-panel config-panel
        :component component-panel]))))


