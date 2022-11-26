(ns bh.ui-component.atom.chart.sankey-chart
  (:require [bh.ui-component.atom.chart.utils :as utils]
            [bh.ui-component.atom.chart.wrapper-2 :as wrapper]
            [bh.ui-component.utils :as ui-utils]
            [bh.ui-component.utils.color :as c]
            [bh.ui-component.utils.color :as color]
            [bh.ui-component.utils.example-data :as data]
            [re-frame.core :as rf]
            ["recharts" :refer [ResponsiveContainer Sankey Tooltip Layer Rectangle Layer]]
            [re-com.core :as rc]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.atom.chart.sankey-chart-2")


(def sample-data
  "the Sankey Chart works best with \"directed acyclic graph data\" so we return the dag-data from utils"
  data/dag-data)


(def sample-config-data data/dag-config-data)


(defn local-config [data]
  ;(log/info "local-config" @data)

  (merge
    {:link    {:curve 0.5}
     :tooltip {:include true}}
    (->> @data
      :nodes
      (map-indexed (fn [idx {:keys [name] :as all}]
                     {name {:key     all
                            :include true
                            :fill    (color/get-color idx)
                            :stroke  (color/get-color idx)}}))
      (into {}))))


(defn config [component-id data]
  ;(log/info "config" component-id data)
  (merge
    ui-utils/default-pub-sub
    (local-config data)
    {:tab-panel {:value     (keyword component-id "config")
                 :data-path [:containers (keyword component-id) :tab-panel]}}))


(defn- color-config [component-id label path position]
  [rc/v-box :src (rc/at)
   :gap "5px"
   :children [[:code label]
              [rc/line :size "2px"]
              [utils/color-config component-id ":fill" (conj path :fill) position]
              [utils/color-config component-id ":stroke" (conj path :stroke) position]]])


(defn- make-config [component-id data]
  ;(log/info "make-config" component-id "//" @data)

  (->> @data
    :nodes
    (map-indexed (fn [idx {:keys [name]}]
                   [color-config component-id name [name] :below-right]))
    (into [])))


(defn config-panel [data component-id]
  ;(log/info "config-panel" data "//" component-id)

  [rc/v-box :src (rc/at)
   :gap "5px"
   :children [[utils/tooltip component-id]
              [rc/line :size "2px"]
              [rc/h-box :src (rc/at)
               :gap "5px"
               :children [[utils/text-config component-id ":curve" [:link :curve]]
                          [utils/slider-config component-id 0 1 0.1 [:link :curve]]]]
              [rc/line :size "2px"]
              [rc/h-box :src (rc/at)
               :width "100%"
               :height "100%"
               :style ui-utils/h-wrap
               :gap "10px"
               :children (make-config component-id data)]]])


(def source-code '[:> Sankey
                   {:width         500 :height 400
                    :node          (partial complex-node 500 @fill)
                    :data          @data
                    :margin        {:top 20 :bottom 20 :left 20 :right 20}
                    :nodeWidth     10
                    :nodePadding   60
                    :linkCurvature @curve
                    :iterations    64
                    :link          {:stroke @stroke}}
                   (when @tooltip? [:> Tooltip])])


(defn- complex-node
  "build the reagent/react components (as hiccup) needed to draw the `node` parts (rectangles)
  and labels of the diagram.

  ---

  - containerWidth : (number) with of the container, used to determine if the label should be to the left or right of the rectangle
  - fill : (color) color to fill the rectangle
  - stroke : (color) color to use as the outline (stroke) of the rectangle
  - props : (has-map) additional props sent to the reagent/react component by the diagram itself

> See [here](https://cljdoc.org/d/reagent/reagent/1.1.0/doc/tutorials/react-features#hooks)
> for details on how the Reagent/React interop work for this
"
  [subscriptions containerWidth props]
  (let [{x                           "x"
         y                           "y"
         width                       "width"
         height                      "height"
         index                       "index"
         {name "name" value "value"} "payload"} (js->clj props)
        isOut  (< containerWidth (+ x width 30 6))]

    ;(log/info "complex-node" name containerWidth props)

    (r/as-element
      [:> Layer {:key (str "CustomNode$" index)}
       [:> Rectangle {:x      x :y y :width width :height height
                      :fill   (ui-utils/resolve-sub subscriptions [name :fill])
                      :stroke (ui-utils/resolve-sub subscriptions [name :stroke])}]
       [:text {:textAnchor (if isOut "end" "start")
               :x          (if isOut (- x 6) (+ x width 6))
               :y          (+ y (/ height 2))
               :fontSize   14
               :stroke     "#333"}
        name]
       [:text {:textAnchor    (if isOut "end" "start")
               :x             (if isOut (- x 6) (+ x width 6))
               :y             (+ y 13 (/ height 2))
               :fontSize      12
               :stroke        "#333"
               :strokeOpacity 0.5}
        (str value "k")]])))


(defn- make-svg-string [sourceX, targetX,
                        sourceY, targetY,
                        sourceControlX, targetControlX,
                        linkWidth]
  (str "M" sourceX ", " (+ sourceY (/ linkWidth 2))

    "C" sourceControlX ", " (+ sourceY (/ linkWidth 2)) ", "
    targetControlX ", " (+ targetY (/ linkWidth 2)) ", "
    targetX ", " (+ targetY (/ linkWidth 2))

    "L" targetX ", " (- targetY (/ linkWidth 2)) ", "

    "C" targetControlX ", " (- targetY (/ linkWidth 2)) ", "
    sourceControlX ", " (- sourceY (/ linkWidth 2)) ","
    sourceX ", " (- sourceY (/ linkWidth 2))

    "Z"))


(defn color-source->white [subscriptions index payload]
  (let [color-from (ui-utils/resolve-sub subscriptions [(get-in payload [:source :name]) :fill])
        c-from     (-> color-from
                     c/hex->rgba
                     (assoc :a 0.5)
                     c/rgba-map->js-function)
        c-to       (-> color-from
                     c/hex->rgba
                     (assoc :a 0.05)
                     c/rgba-map->js-function)]
    [:defs
     [:linearGradient {:id (str "linkGradient$" index)}
      [:stop {:offset "0%" :stopColor c-from}]
      [:stop {:offset "100%" :stopColor c-to}]]]))


(defn color-white->target [subscriptions index payload]
  (let [color-to (ui-utils/resolve-sub subscriptions [(get-in payload [:target :name]) :fill])
        c-from   (-> color-to
                   c/hex->rgba
                   (assoc :a 0.05)
                   c/rgba-map->js-function)
        c-to     (-> color-to
                   c/hex->rgba
                   (assoc :a 0.5)
                   c/rgba-map->js-function)]
    [:defs
     [:linearGradient {:id (str "linkGradient$" index)}
      [:stop {:offset "0%" :stopColor c-from}]
      [:stop {:offset "100%" :stopColor c-to}]]]))


(defn color-source->target [subscriptions index payload]
  (let [color-from (ui-utils/resolve-sub subscriptions [(get-in payload [:source :name]) :fill])
        color-to   (ui-utils/resolve-sub subscriptions [(get-in payload [:target :name]) :fill])
        c-from     (-> color-from
                     c/hex->rgba
                     (assoc :a 0.5)
                     c/rgba-map->js-function)
        c-mid      (-> color-from
                     c/hex->rgba
                     (assoc :a 0.2)
                     c/rgba-map->js-function)
        c-to       (-> color-to
                     c/hex->rgba
                     (assoc :a 0.3)
                     c/rgba-map->js-function)]
    [:defs
     [:linearGradient {:id (str "linkGradient$" index)}
      [:stop {:offset "0%" :stopColor c-from}]
      [:stop {:offset "30%" :stopColor c-mid}]
      [:stop {:offset "100%" :stopColor c-to}]]]))


(defn- complex-link [subscriptions link-color-fn props]
  (let [{:keys [sourceX, targetX,
                sourceY, targetY,
                sourceControlX, targetControlX,
                linkWidth,
                index, payload]} (js->clj props :keywordize-keys true)]

    ;(log/info "complex-link (props)" (js->clj props :keywordize-keys true))

    (r/as-element
      [:> Layer {:key (str "CustomLink$" index)}

       (link-color-fn subscriptions index payload)

       [:path {:d           (make-svg-string
                              sourceX, targetX,
                              sourceY, targetY,
                              sourceControlX, targetControlX,
                              linkWidth)
               ;:fill        c-from
               :fill        (str "url(#linkGradient$" index ")")
               :strokeWidth 0}]])))


(defn- ->sankey [data]
  (assoc data
    :nodes (->> data :nodes (sort-by :index) (into []))
    :links (->> data
             :links
             (map (fn [{:keys [source target] :as link}]
                    (assoc link :source (->> data
                                          :nodes
                                          (filter #(= (:name %) source))
                                          first
                                          :index)
                                :target (->> data
                                          :nodes
                                          (filter #(= (:name %) target))
                                          first
                                          :index)))))))


(defn- component* [& {:keys [data component-id link-color-fn subscriptions]
                      :as   params}]

  ;(log/info "component-star" component-id
  ;"//" data
  ;"//" subscriptions
  ;"//" link-color-fn

  (let [tooltip?    (ui-utils/resolve-sub subscriptions [:tooltip :include])
        curve       (ui-utils/resolve-sub subscriptions [:link :curve])]

    ; NOTE: super hack here!!! we need the config-data change to force a re-render
    ; because the sankey only redraws the nodes and links when the cursor moves over them
    ; otherwise
    [:div {:class
           (subs (str @(ui-utils/subscribe-local component-id [])) 0 10)
           :style {:width "100%" :height "100%"}}

     [:> ResponsiveContainer
      [:> Sankey
       {:node          (partial complex-node subscriptions 700)
        :data          (->sankey data)
        :margin        {:top 20 :bottom 20 :left 20 :right 20}
        :nodeWidth     10
        :nodePadding   60
        :linkCurvature curve
        :iterations    64
        :link          (partial complex-link subscriptions (or link-color-fn color-source->white))}
       (when tooltip? [:> Tooltip])]]]))


(defn component [& {:keys [component-id] :as params}]

  ;(log/info "component" params)

  (let [input-params (assoc params :component-panel wrapper/component-panel
                                   :config config
                                   :local-config local-config
                                   :component* component*)]
    (reduce into [wrapper/base-chart] input-params)))


(def meta-data {:rechart/sankey {:component component
                                 ;:configurable-component configurable-component
                                 :ports     {:data   :port/sink
                                             :config :port/sink}}})


(rf/dispatch-sync [:register-meta meta-data])






(comment
  (def component-id "sankey-chart-demo")

  ())