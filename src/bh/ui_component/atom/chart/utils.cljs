(ns bh.ui-component.atom.chart.utils
  (:require [bh.events :as events]
            [bh.ui-component.atom.re-com.table :as table]
            [bh.ui-component.utils :as u]
            [bh.ui-component.utils.color :as color]
            [bh.ui-component.utils.helpers :as h]
            [re-com.box :as rcb]
            [re-com.core :as rc]
            [re-com.debug :as rc-dbg]
            [re-com.util :as rcu]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [taoensso.timbre :as log]
            [woolybear.packs.tab-panel :as tab-panel]

            ["recharts" :refer [XAxis YAxis CartesianGrid Tooltip Legend]]
            ["react-colorful" :refer [HexColorPicker]]))


(log/info "bh.ui-component.atom.chart.utils")


(defn init-config-panel
  "this need some REALLY GOOD documentation!"
  [base-id]
  ;(log/info "init-config-panel" base-id)
  (let [formal-id (keyword base-id)
        data-path [formal-id :tab-panel]
        config-id (keyword base-id "config")
        data-id   (keyword base-id "data")
        db-id     (keyword "db" base-id)
        tab-id    (keyword base-id "tab-panel")
        value-id  (keyword base-id "value")
        init-db   {:tab-panel (tab-panel/mk-tab-panel-data
                                data-path config-id)}]

    (re-frame/reg-sub
      db-id
      (fn [db _]
        (formal-id db)))

    (re-frame/reg-sub
      tab-id
      :<- [db-id]
      (fn [navbar]
        (:tab-panel navbar)))

    (re-frame/reg-sub
      value-id
      :<- [tab-id]
      (fn [tab-panel]
        (:value tab-panel)))

    (re-frame/dispatch-sync [::events/init-locals formal-id init-db])))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; DATA DISPLAY/EDIT PANELS
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; region

(defn dummy-data-panel [data]
  [:div "a dummy data panel"])


(defn tabular-data-panel
  "provides a simple tabular component (via `bh.ui-component.table`) to show the data presented
  in the Chart.

> Note: `table` uses the keys of the first hash-map in `@data` as the header label for the columns

  ---

  - data : (atom) vector of content hash-maps."

  [data]
  ;(log/info "tabular-data-panel" @data)
  [table/table
   :width 500
   :data @data
   :max-rows 5])


(defn meta-tabular-data-panel
  "provides a simple tabular component (via `bh.ui-component.table`) to show the data presented
  in the Chart.

> Note: `table` uses the keys of the first hash-map in `@data` as the header label for the columns

  ---

  - data : (atom) atom wrapping data with metadata included"

  [data]
  ;(log/info "meta-tabular-data-panel" @data)
  [table/meta-table
   :width 500
   :data data
   :max-rows 5])


(defn dag-data-panel
  "provides a UI component to show the DAG data presented in the Chart.

> Note: `table` uses the keys of the first hash-map in `@data` as the header label for the columns

  ---

  - data : (atom) vector of content hash-maps."

  [data]
  [:div "DAG data will be shown here"])


(defn hierarchy-data-panel
  "provides a UI component to show the hierarchical data presented in the Chart.

  ---

  - data : (atom) data to show/edit"

  [data]
  [:div "hierarchical data will be shown here"])

;; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; LOW-LEVEL CONFIGURATION 'ATOMS' & 'MOLECULES'
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; region

(def btns-style {:font-size   "12px"
                 :line-height "20px"
                 :padding     "6px 8px"})
(def x-axis-btns [{:id :bottom :label ":bottom"}
                  {:id :top :label ":top"}])
(def y-axis-btns [{:id :left :label ":left"}
                  {:id :right :label ":right"}])
(def default-config {:isAnimationActive true
                     :grid              {:include         true
                                         :strokeDasharray {:dash "3" :space "3"}
                                         :stroke          "#a9a9a9"}
                     :x-axis            {:include     true
                                         :dataKey     ""
                                         :orientation :bottom
                                         :scale       "auto"}
                     :y-axis            {:include     true
                                         :dataKey     ""
                                         :orientation :left
                                         :scale       "auto"}
                     :tooltip           {:include true}
                     :legend            {:include       true
                                         :layout        "horizontal"
                                         :align         "center"
                                         :verticalAlign "bottom"}})


(defn- subscription-error [label path]
  (log/info (str label " : " path " - error"))
  [:div])


(defn column-picker [data component-id label path]
  (let [model (u/subscribe-local component-id path)]
    (fn []
      (if model
        (let [headings (apply set (map keys (get @data :data)))
              btns     (mapv (fn [h] {:id h :label h}) headings)]
          ;(log/info "column-picker" data "//" component-id "//" label "//" path)
          [rc/h-box :src (rc/at)
           :gap "5px"
           :children [[rc/box :src (rc/at) :align :start :child [:code label]]
                      [rc/horizontal-bar-tabs
                       :src (rc/at)
                       :model @model
                       :tabs btns
                       :style btns-style
                       :on-change #(u/dispatch-local component-id path %)]]])
        (subscription-error label path)))))


(defn- multi-button [data component-id path item]
  (log/info "multi-button" item)
  [:div (str item)])


(defn multi-bar-tabs
  [& {:keys [model btns on-change id-fn label-fn tooltip-fn tooltip-position
             vertical? class style attr parts validate?]
      :or   {id-fn :id label-fn :label tooltip-fn :tooltip}
      :as args}]
  (let [showing (r/atom nil)]
    (fn [& {:keys [model tabs]}]
      (let [chosen (rcu/deref-or-value model)
            btns   (rcu/deref-or-value btns)
            _      (assert (or (not validate?) (not-empty (filter #(= chosen (id-fn %)) tabs))) "model not found in tabs vector")]

        (log/info "multi-bar-tabs" model "//" chosen "//" btns)

        (into [:div
               (merge
                 {:class (str "noselect btn-group" (if vertical? "-vertical") " rc-tabs " class)
                  :style (merge (rcb/flex-child-style "none")
                           (get-in parts [:wrapper :style]))}
                 (rc-dbg/->attr args)
                 attr)]
          (for [t btns]
            (let [id         (id-fn t)
                  label      (label-fn t)
                  tooltip    (when tooltip-fn (tooltip-fn t))
                  selected?  (contains? chosen id)
                  the-button [:button
                              (merge
                                {:type     "button"
                                 :key      (str id)
                                 :class    (str "btn btn-default " (if selected? "active ") "rc-tabs-btn " (get-in parts [:button :class]))
                                 :style    style
                                 :on-click (when on-change (rc/handler-fn (on-change id)))}
                                (when tooltip
                                  {:on-mouse-over (rc/handler-fn (reset! showing id))
                                   :on-mouse-out  (rc/handler-fn (swap! showing #(when-not (= id %) %)))})
                                (get-in parts [:button :attr]))
                              label]]
              (if tooltip
                [rc/popover-tooltip
                 :src (rc/at)
                 :label tooltip
                 :position (or tooltip-position :below-center)
                 :showing? (r/track #(= id @showing))
                 :anchor the-button
                 :class (str "rc-tabs-tooltip " (get-in parts [:tooltip :class]))
                 :style (get-in parts [:tooltip :style])
                 :attr (get-in parts [:tooltip :attr])]
                the-button))))))))



(defn column-multi-picker [data component-id label path]
  (let [model (u/subscribe-local component-id path)]
    (fn []
      (if model
        (let [headings (apply set (map keys (get @data :data)))
              btns     (mapv (fn [h] {:id h :label h}) headings)
              as-set   (set @model)]
          ;(log/info "column-multi-picker" headings "//" as-set)
          [rc/h-box :src (rc/at)
            :gap "5px"
            :children [[rc/box :src (rc/at) :align :start :child [:code label]]
                       [multi-bar-tabs
                        :model (r/atom as-set)
                        :btns btns]]])
        (subscription-error label path)))))


(defn boolean-config
  "lets the user turn on/of some component of the Chart

  ---

  - config : (atom) holds a hash-map of the actual configuration properties see [[config]].
  - label : (string) tell the user which subcomponent this control is manipulating
  - path : (vector) path into `config` where the subcomponent 'inclusion' value is stored
  "
  [config label path]

  (let [checked? (u/subscribe-local config path)]
    (fn []
      (if checked?
        [rc/checkbox :src (rc/at)
         :label (cond
                  (and (string? label) (empty? label)) ""
                  :else [rc/box :src (rc/at) :align :start :child [:code label]])
         :model @checked?
         :on-change #(u/dispatch-local config path %)]
        (subscription-error label path)))))


(defn slider-config
  ([component-id min max step path]

   ;(log/info "slider-config" component-id min max step path)

   (let [model (u/subscribe-local component-id path)]
     (fn []
       ;(log/info "slider-config (model)" model "//" (when model @model))

       (if (and model @model)                               ; needed to cover possible race condition where the subscription initially returns nil
         [rc/slider :src (rc/at)
          :model @model
          :width "100px"
          :min min :max max :step step
          :on-change #(u/dispatch-local component-id path %)]
         (subscription-error "slider" path)))))

  ([component-id min max path]
   [slider-config component-id min max 1 path]))


(defn text-config [component-id label path]
  (let [model (u/subscribe-local component-id path)]
    (fn []
      (if model
        [rc/h-box :src (rc/at)
         :gap "5px"
         :children [[rc/label :src (rc/at) :label label]
                    [rc/input-text :src (rc/at)
                     :model (str @model)
                     :width "50px"
                     :on-change #(u/dispatch-local component-id path %)]]]
        (subscription-error label path)))))


(defn strokeDasharray
  "reconstitutes the 2-part string value required by `:strokeDasharray` from the
  2 values in the [[config]] atom.

  ---

  - dash
  - space
  "
  [dash & space]
  (str dash " " (first space)))


(defn dashArray-config
  "provides the user with 2 sliders to control the 2 parts of the `:strokeDasharray`
  property of a chart's [`CartesianGrid`](https://recharts.org/en-US/api/CartesianGrid)

  ---

  - config : (atom) holds a hash-map of the actual configuration properties see [[config]].
  - label : (string) tell the user which axis this control is manipulating
  - min : (integer) minimum value for the slider
  - max : (integer) maximum value for the slider
  - path : (vector) path into `config` where the :strokeDasharray is stored
  "

  [component-id label min max path]
  [rc/h-box :src (rc/at)
   :children [[rc/box :src (rc/at) :align :start :child [:code label]]
              [rc/v-box :src (rc/at)
               :gap "5px"
               :children [[slider-config component-id min max (conj path :dash)]
                          [slider-config component-id min max (conj path :space)]]]]])


(defn enumerated-config
  "provides a multi-button control for setting a property fro a set of mutually-exclusive options

  ---

  - component-id : (string/keyword) unique ID for this component
  - btns : (vector) define the button(s) that set the value(s).

  | key       | description                                                          |
  |:----------|:---------------------------------------------------------------------|
  | `:id`     | the value to set when the use click the corresponding button control |
  | `:label`  | the label to put on the button                                       |


  - label : (string) tell the user what property is being manipulated
  - path : (vector) path into `config` for the correct property

  "
  [component-id btns label path]

  (let [model (u/subscribe-local component-id path)]
    (fn []
      (if model
        [rc/h-box :src (rc/at)
         :children [[rc/box :src (rc/at) :align :start :child [:code label]]
                    [rc/horizontal-bar-tabs
                     :src (rc/at)
                     :model @model
                     :tabs btns
                     :style btns-style
                     :on-change #(u/dispatch-local component-id path %)]]]
        (subscription-error label path)))))


(defn orientation-config
  "lets the user configure the orientation of an axis. Which axis is defined by the arguments.

  ---

  - component-id : (string/keyword) unique ID for this component
  - btns : (vector) define the button that set the value(s).

  | key       | description                                                          |
  |:----------|:---------------------------------------------------------------------|
  | `:id`     | the value to set when the use click the corresponding button control |
  | `:label`  | the label to put on the button                                       |

  Each axis support a different set of possible orientations:

  | axis      | allowed orientations   |
  |:----------|:-----------------------|
  | X Axis    | `:top`  `:bottom`     |
  | Y Axis    | `:left`  `:right`     |

  - label : (string) tell the user which axis this control is manipulating
  - path : (vector) path into `config` where the orientation for the correct axis is stored
  "
  [component-id btns label path]

  (enumerated-config component-id btns label path))


(defn scale-config
  "lets the user change the scale of an 'axis'. Which axis is defined by the arguments.
  Supports only:

    `auto`  `linear`  `pow`  `sqrt`  `log`

  scale types. Recharts supports many more. See [here](https://recharts.org/en-US/api/XAxis#scale)

  ---

  - component-id : id for the component.
  - label : (string) tell the user which axis this control is manipulating
  - path : (vector) path into `config` where the scale for the correct axis is stored
  "
  [component-id label path]
  (let [btns [{:id "auto" :label "auto"}
              {:id "linear" :label "linear"}
              {:id "pow" :label "pow"}
              {:id "sqrt" :label "sqrt"}
              {:id "log" :label "log"}]]
    (enumerated-config component-id btns label path)))


(defn layout-config
  "lets the user change the layout of a 'legend'.
  Supports:

    `horizontal`  &  `linear`

  ---

  - component-id : id of the component
  - path : (vector) path into `config` where the scale for the layout is stored
  "
  [component-id path]
  (let [btns [{:id "horizontal" :label "horizontal"}
              {:id "vertical" :label "vertical"}]]
    (enumerated-config component-id btns ":layout" path)))


(defn align-config
  "lets the user change the alignment of a 'legend'.
  Supports:

    `left`  `center`  `right`

  ---

  - component-id : id of the component
  - path : (vector) path into `config` where the scale for the layout is stored
  "
  [component-id path]
  (let [btns [{:id "left" :label "left"}
              {:id "center" :label "center"}
              {:id "right" :label "right"}]]
    (enumerated-config component-id btns ":align" path)))


(defn verticalAlign-config
  "lets the user change the vertical alignment of a 'legend'.
  Supports:

    `top`  `middle`  `bottom`

  ---

  - component-id : id of the component
  - path : (vector) path into `config` where the scale for the layout is stored
  "
  [component-id path]
  (let [btns [{:id "top" :label "top"}
              {:id "middle" :label "middle"}
              {:id "bottom" :label "bottom"}]]
    (enumerated-config component-id btns ":verticalAlign" path)))


(defn color-config [config-data label path & [position]]
  (let [d        (h/resolve-value config-data)
        showing? (r/atom false)
        p        (or position :right-center)]

    ;(log/info "color-config" label "//" config-data "//" @d "//" path "//" @showing?)

    (fn []
      [rc/popover-anchor-wrapper :src (rc/at)
       :showing? @showing?
       :position p
       :anchor [rc/button :src (rc/at)
                :label label
                :style {:background-color (get-in @d path)
                        :color            (color/best-text-color
                                            (color/hex->rgba (get-in @d path)))}
                :on-click #(swap! showing? not)]
       :popover [rc/popover-content-wrapper :src (rc/at)
                 :close-button? false
                 :no-clip? false
                 :body [:> HexColorPicker {:color     (get-in @d path)
                                           :on-change #(h/handle-change-path config-data path %)}]]])))


(defn color-config-text [component-id label path & [position]]
  ;(log/info "color-config-text" component-id "//" label "//" path)

  (let [model (u/subscribe-local component-id path)]
    (fn []
      (if model
        [rc/h-box :src (rc/at)
         :gap "5px"
         :children [[color-config component-id label path position
                     [rc/input-text :src (rc/at)
                      :width "100px"
                      :model @model
                      :on-change #(u/dispatch-local component-id path %)]]]]
        (subscription-error label path)))))


;; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; HIGH-LEVEL CONFIGURATION 'MOLECULES'
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; region

(defn isAnimationActive [component-id]
  [boolean-config component-id ":isAnimationActive" [:isAnimationActive]])


(defn grid [component-id]
  [rc/v-box :src (rc/at)
   :children [[boolean-config component-id ":grid" [:grid :include]]
              [dashArray-config component-id
               ":strokeDasharray" 1 10 [:grid :strokeDasharray]]
              [color-config-text component-id ":stroke" [:grid :stroke]]]])


(defn x-axis [data component-id]
  [rc/v-box :src (rc/at)
   :children [[boolean-config component-id ":x-axis" [:x-axis :include]]
              [column-picker data component-id ":dataKey" [:x-axis :dataKey]]
              [orientation-config component-id x-axis-btns ":orientation" [:x-axis :orientation]]
              [scale-config component-id ":scale" [:x-axis :scale]]]])


(defn y-axis [data component-id]
  [rc/v-box :src (rc/at)
   :children [[boolean-config component-id ":y-axis" [:y-axis :include]]
              [column-picker data component-id ":dataKey" [:y-axis :dataKey]]
              [orientation-config component-id y-axis-btns ":orientation" [:y-axis :orientation]]
              [scale-config component-id ":scale" [:y-axis :scale]]]])


(defn tooltip [component-id]
  [boolean-config component-id ":tooltip" [:tooltip :include]])


(defn legend [component-id]
  [rc/v-box :src (rc/at)
   :children [[boolean-config component-id ":legend" [:legend :include]]
              [layout-config component-id [:legend :layout]]
              [align-config component-id [:legend :align]]
              [verticalAlign-config component-id [:legend :verticalAlign]]]])


(defn option [component-id label path-root]
  (let [chosen-path (conj path-root :chosen)
        keys-path   (conj path-root :keys)
        chosen      (u/subscribe-local component-id chosen-path)
        keys        (u/subscribe-local component-id keys-path)
        btns        (->> @keys
                      (map (fn [k]
                             {:id k :label k})))]

    ;(log/info "option" @keys @chosen btns)

    (fn []
      [rc/h-box :src (rc/at)
       :children [[rc/box :src (rc/at) :align :start :child [:code label]]
                  [rc/horizontal-bar-tabs
                   :src (rc/at)
                   :model @chosen
                   :tabs btns
                   :style btns-style
                   :on-change #(u/dispatch-local component-id chosen-path %)]]])))

;; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; VERY!!! HIGH-LEVEL CONFIGURATION 'MOLECULES'
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; region

(defn standard-chart-config [data component-id]
  [:<>
   [isAnimationActive component-id]
   [rc/line :src (rc/at) :size "2px"]
   [grid component-id]
   [rc/line :src (rc/at) :size "2px"]
   [x-axis data component-id]
   [rc/line :src (rc/at) :size "2px"]
   [y-axis data component-id]
   [rc/line :src (rc/at) :size "2px"]
   [tooltip component-id]
   [rc/line :src (rc/at) :size "2px"]
   [legend component-id]])


(defn non-gridded-chart-config [data component-id]
  [:<>
   [isAnimationActive component-id]
   [rc/line :src (rc/at) :size "2px"]
   [tooltip component-id]
   [rc/line :src (rc/at) :size "2px"]
   [legend component-id]])


;; endregion

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; HIGH-LEVEL COMPONENT 'MOLECULES'
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; region


(defn override [s ui tag]
  ;(log/info "override" s "///" ui "///" tag)
  (if (and
        (seq ui)
        (not (empty? ui))
        (contains? (into #{} (keys ui)) tag))
    (get ui tag)
    s))


(defn chart-grid [component-id ui]
  (let [grid?       (u/subscribe-local component-id [:grid :include])
        grid-dash   (u/subscribe-local component-id [:grid :strokeDasharray :dash])
        grid-space  (u/subscribe-local component-id [:grid :strokeDasharray :space])
        grid-stroke (u/subscribe-local component-id [:grid :stroke])]
    (when (override @grid? ui :grid) [:> CartesianGrid {:strokeDasharray (strokeDasharray @grid-dash @grid-space)
                                                        :stroke          @grid-stroke}])))


(defn standard-chart-components [component-id ui]

  ;(log/info "standard-chart-components" component-id ui)

  (let [grid?                (u/subscribe-local component-id [:grid :include])
        grid-dash            (u/subscribe-local component-id [:grid :strokeDasharray :dash])
        grid-space           (u/subscribe-local component-id [:grid :strokeDasharray :space])
        grid-stroke          (u/subscribe-local component-id [:grid :stroke])

        x-axis?              (u/subscribe-local component-id [:x-axis :include])
        x-axis-dataKey       (u/subscribe-local component-id [:x-axis :dataKey])
        x-axis-orientation   (u/subscribe-local component-id [:x-axis :orientation])
        x-axis-scale         (u/subscribe-local component-id [:x-axis :scale])

        y-axis?              (u/subscribe-local component-id [:y-axis :include])
        y-axis-dataKey       (u/subscribe-local component-id [:y-axis :dataKey])
        y-axis-orientation   (u/subscribe-local component-id [:y-axis :orientation])
        y-axis-scale         (u/subscribe-local component-id [:y-axis :scale])

        tooltip?             (u/subscribe-local component-id [:tooltip :include])

        legend?              (u/subscribe-local component-id [:legend :include])
        legend-layout        (u/subscribe-local component-id [:legend :layout])
        legend-align         (u/subscribe-local component-id [:legend :align])
        legend-verticalAlign (u/subscribe-local component-id [:legend :verticalAlign])]

    [:<>
     (when (override @grid? ui :grid) [:> CartesianGrid {:strokeDasharray (strokeDasharray @grid-dash @grid-space)
                                                         :stroke          @grid-stroke}])

     (when (override @x-axis? ui :x-axis) [:> XAxis {:dataKey     @x-axis-dataKey
                                                     :orientation @x-axis-orientation
                                                     :scale       @x-axis-scale}])

     (when (override @y-axis? ui :y-axis) [:> YAxis {:dataKey     @y-axis-dataKey
                                                     :orientation @y-axis-orientation
                                                     :scale       @y-axis-scale}])

     (when (override @tooltip? ui :tooltip) [:> Tooltip])

     (when (override @legend? ui :legend) [:> Legend {:layout        @legend-layout
                                                      :align         @legend-align
                                                      :verticalAlign @legend-verticalAlign}])]))


(defn non-gridded-chart-components [component-id ui]
  (let [tooltip?             (u/subscribe-local component-id [:tooltip :include])
        legend?              (u/subscribe-local component-id [:legend :include])
        legend-layout        (u/subscribe-local component-id [:legend :layout])
        legend-align         (u/subscribe-local component-id [:legend :align])
        legend-verticalAlign (u/subscribe-local component-id [:legend :verticalAlign])]

    ;(log/info "non-gridded-chart-components" component-id ui)

    [:<>
     (when (override @tooltip? ui :tooltip) [:> Tooltip])

     (when (override @legend? ui :legend) [:> Legend {:layout        @legend-layout
                                                      :align         @legend-align
                                                      :verticalAlign @legend-verticalAlign}])]))

;; endregion


; workout the override logic for chart elements like grid, legend, etc.
(comment
  (def ui {:tooltip false})
  (def ui nil)
  (def ui "")
  (def ui {:grid false, :x-axis false, :y-axis false, :legend false, :tooltip false})
  (def tag :tooltip)
  (def tag :grid)
  (def tooltip? (r/atom true))
  (def grid? (r/atom true))
  (def s @tooltip?)
  (def s @grid?)

  (first ui)

  (if (and (seq ui) (not (empty? (first ui)))) true false)


  (if (and
        (seq ui)
        (not (empty? ui))
        (contains? (into #{} (keys ui)) tag))
    (get ui tag)
    s)


  (if nil true false)
  (or true nil)

  (override @tooltip? ui :tooltip)
  (override @grid? ui :grid)

  ())
