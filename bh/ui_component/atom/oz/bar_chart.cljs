(ns bh.ui-component.atom.oz.bar-chart
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

           :data    [{:name   "table"
                      :values [{:category "A" :amount 28}
                               {:category "B" :amount 55}
                               {:category "C" :amount 43}
                               {:category "D" :amount 91}
                               {:category "E" :amount 81}
                               {:category "F" :amount 53}
                               {:category "G" :amount 19}
                               {:category "H" :amount 87}]}]

           :signals [{:name  :tooltip
                      :value {}
                      :on    [{:events "rect:mouseover" :update "datum"}
                              {:events "rect:mouseout" :update "{}"}]}]

           :scales  [{:name    :x-axis
                      :type    :band
                      :domain  {:data "table" :field :category}
                      :range   :width
                      :padding 0.05
                      :round   true}
                     {:name   :y-axis
                      :domain {:data "table" :field :amount}
                      :nice   true
                      :range  :height}]

           :axes    [{:orient :bottom :scale :x-axis}
                     {:orient :left :scale :y-axis}]

           :marks   [{:type   "rect"
                      :from   {:data "table"}
                      :encode {:enter  {:x     {:scale :x-axis :field :category}
                                        :width {:scale :x-axis :band 1}
                                        :y     {:scale :y-axis :field :amount}
                                        :y2    {:scale :y-axis :value 0}}
                               :update {:fill {:value "steelblue"}}
                               :hover  {:fill {:value "red"}}}}

                     {:type   "text"
                      :encode {:enter  {:align {:value    :center
                                                :baseline {:value :bottom}
                                                :fill     {:value "#333"}}}
                               :update {:x           {:scale :x-axis :signal "tooltip.category" :band 0.5}
                                        :y           {:scale :y-axis :signal "tooltip.amount" :offset -2}
                                        :text        {:signal "tooltip.amount"}
                                        :fillOpacity [{:test "isNaN(tooltip.amount)" :value 0}
                                                      {:value 1}]}}}]}))


(defn- component-panel
  [data component-id]

  [:div {:style {:width "400px" :height "500px"}}
   [oz/vega-lite @data]])


(defn component
  ([& {:keys [data component-id container-id]}]

   ;(log/info "bar-chart" @data)

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


(comment
  [:div
   [oz.core/vega {}]
   [oz.core/vega-lite {}]]


  ())

