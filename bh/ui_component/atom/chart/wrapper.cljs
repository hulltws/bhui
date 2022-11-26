(ns bh.ui-component.atom.chart.wrapper
  (:require [bh.ui-component.utils :as ui-utils]
            [bh.ui-component.atom.re-com.configure-toggle :as ct]
            [bh.ui-component.utils.helpers :as h]
            [re-com.core :as rc]
            [reagent.core :as r]
            [taoensso.timbre :as log]
            [woolybear.ad.layout :as layout]))


(defn configurable-chart
  "takes a component and wraps it with a 'settings' button which can show/hide the
  appropriate data and config panels.

  ---

  - data : (atom) the data to be shown
  - config : (hash-map) the data structure providing the local state for the component and the config-panel
  - id : (string) unique identifier for this component instance
  - config-panel : (hiccup) the configuration 'molecule' for the component
  - component : (hiccup) the component itself

  Returns : (hiccup) the Reagent component representing the entire 'package' (component + config-panel + button)
  "
  [& {:keys [data component-id container-id config-panel data-panel component ui]}]

  ;(log/info "configurable-chart" data "//" component-id "//" container-id "//" component)

  (let [open? (r/atom false)
        config-key (keyword component-id "config")
        data-key (keyword component-id "data")
        tab-panel (ui-utils/path->keyword component-id "tab-panel")
        selected-tab (ui-utils/path->keyword component-id "tab-panel.value")
        chart-events [config-key data-key tab-panel selected-tab]]

    ;(log/info "configurable-chart" component-id
    ;  "///" container-id "///" ui
    ;  "///" chart-events)

    (ui-utils/dispatch-local component-id [:container] container-id)

    (fn [& {:keys [data component-id config-panel component ui]}]
      [rc/v-box :src (rc/at)
       :gap "2px"
       :children [[rc/h-box :src (rc/at)
                   :justify :end
                   :children [[ct/configure-toggle open?]]]
                  [layout/centered {:extra-classes :is-one-third}
                   [rc/h-box :src (rc/at)
                    :gap "5px"
                    :width "600px"
                    :height "600px"
                    :children (conj
                                (if @open?
                                  [[layout/centered {:extra-classes :is-one-third}
                                    [:div {:width "75%"}
                                     [ui-utils/chart-config
                                      chart-events
                                      [data-panel data]
                                      [config-panel data component-id]]]]]
                                  [])
                                [component data component-id container-id ui])]]]])))


(defn chart [& {:keys [data component-id container-id component ui]}]
  (ui-utils/dispatch-local component-id [:container] container-id)

  (let [d (h/resolve-value data)]
    ;(log/info "chart" component-id "//" container-id
    ;  "//" data "//" @d
    ;  "//" ui)

    [component data component-id container-id ui]))


(defn base-chart [& {:keys [data config
                            component-id container-id
                            data-panel config-panel component-panel
                            ui]}]

  ;(log/info "base-chart" component-id container-id)

  (let [id (r/atom nil)
        not-configurable? (nil? config-panel)
        d (h/resolve-value data)]

    ;(log/info "base-chart"
    ;  component-id container-id
    ;  "//" data "//" @d)

    (fn []
      (when (nil? @id)
        ;(log/info "initializing" component-id)
        (reset! id component-id)
        (ui-utils/init-container-locals @id config)
        (ui-utils/dispatch-local @id [:container] container-id))

      (if not-configurable?
        [chart
         :data data
         :component-id @id
         :container-id container-id
         :component component-panel
         :ui ui]

        [configurable-chart
         :data d
         :component-id @id
         :container-id container-id
         :data-panel data-panel
         :config-panel config-panel
         :component component-panel
         :ui ui]))))



(comment
  (do
    (def id "line-chart-demo.line-chart")
    (def container-id "line-chart-demo")
    (def config {:tv {:include true, :stroke "#82ca9d", :fill "#82ca9d"},
                 :brush false,
                 :y-axis {:include true, :dataKey "", :orientation :left, :scale "auto"},
                 :sub :something-selected,
                 :grid {:include true, :strokeDasharray {:dash "3", :space "3"}, :stroke "#a9a9a9"},
                 :legend {:include true, :layout "horizontal", :align "center", :verticalAlign "bottom"},
                 :type "line-chart",
                 :amt {:include true, :stroke "#ff00ff", :fill "#ff00ff"},
                 :tab-panel {:value :line-chart-demo.line-chart.config, :data-path [:containers :line-chart-demo.line-chart :tab-panel]},
                 :pv {:include true, :stroke "#ffc107", :fill "#ffc107"},
                 :container "",
                 :x-axis {:include true, :dataKey :name, :orientation :bottom, :scale "auto"},
                 :pub :name,
                 :uv {:include true, :stroke "#8884d8", :fill "#8884d8"},
                 :tooltip {:include true},
                 :isAnimationActive true}))

  (ui-utils/init-container-locals id config)
  (ui-utils/dispatch-local id [:container] container-id)

  @(ui-utils/subscribe-local id [:container])



  ())

(comment

  (def id "multi-chart-demo/multi-chart/line-chart")
  (def container-id "multi-chart-demo/multi-chart")

  @(ui-utils/subscribe-local id [:container])

  (ui-utils/dispatch-local id [:container] container-id)


  ())