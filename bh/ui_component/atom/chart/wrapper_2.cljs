(ns bh.ui-component.atom.chart.wrapper-2
  (:require [bh.ui-component.atom.re-com.configure-toggle :as ct]
            [bh.ui-component.utils :as ui-utils]
            [bh.ui-component.utils.helpers :as h]
            [bh.ui-component.utils.locals :as l]
            [re-com.core :as rc]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.atom.chart.wrapper-2")


(defn component-panel [& {:keys [data config-data component-id component* local-config] :as params}]
  ;(log/info "component-panel" params)

  (let [d                  (h/resolve-value data)
        c                  (h/resolve-value config-data)
        isAnimationActive? (ui-utils/subscribe-local component-id [:isAnimationActive])]

    ;(log/info "component-panel" component-id
    ;  "// (data)" data "// (d)" @d
    ;  "// (config-data)" config-data "// (c)" @c)

    ;(log/info "component-panel (override)" override-subs)

    (fn []

      (l/update-local-values component-id (local-config d))

      (let [l-c           (local-config d)
            local-subs    (ui-utils/build-subs component-id l-c)
            override-subs (when config-data (l/process-locals [] nil @c))
            subscriptions (if config-data
                            (ui-utils/override-subs @c local-subs override-subs)
                            local-subs)
            input-params (assoc params :data @d
                                       :isAnimationActive? isAnimationActive?
                                       :subscriptions subscriptions)]

        ;(log/info "component-panel (render)" @c
          ;"// (local-config)" l-c
         ;"// (override)" override-subs
         ;"// (subscriptions)" subscriptions
         ;"// (local-subs)" local-subs

        (if (empty? @d)
          [rc/alert-box :src (rc/at)
           :alert-type :info
           :style {:width "100%" :height "100%"}
           :heading "Waiting for data"]

          (reduce into [component*] (seq input-params)))))))


(defn configurable-component-panel [& {:keys [data component-id
                                              config local-config
                                              config-panel data-panel] :as params}]

  ;(log/info "configurable-component-panel" params)

  (let [open?        (r/atom false)
        config-key   (keyword component-id "config")
        data-key     (keyword component-id "data")
        tab-panel    (ui-utils/path->keyword component-id "tab-panel")
        selected-tab (ui-utils/path->keyword component-id "tab-panel.value")
        chart-events [config-key data-key tab-panel selected-tab]]

    ;(log/info "configurable-component" component-id "//" data)

    (ui-utils/init-container-locals component-id (config component-id (h/resolve-value data)))

    (fn []
      (let [d (h/resolve-value data)]

        ;(log/info "configurable-component (INNER)" data "//" @d)

        (l/update-local-values component-id (local-config d))

        [rc/v-box :src (rc/at)
         :class "configurable-component-panel"
         :gap "2px"
         :width "100%"
         :height "100%"
         :children [[rc/h-box :src (rc/at)
                     :class "chart-config-tools"
                     :justify :end
                     :children [[ct/configure-toggle open?]]]
                    [rc/h-box :src (rc/at)
                     :class "chart-itself"
                     :gap "5px"
                     :width "100%"
                     :height "90%"
                     :children (if @open?
                                 [[:div.chart-config-panel {:style {:width "40%" :height "100%"}}
                                   [ui-utils/chart-config
                                    chart-events
                                    [data-panel d]
                                    [config-panel d component-id]]]
                                  [:div.chart-content {:style {:width "60%" :height "100%"}}
                                   (reduce into [component-panel] (seq params))]]

                                 [[:div.chart-content {:style {:width "100%" :height "100%"}}
                                   (reduce into [component-panel] (seq params))]])]]]))))


(defn base-chart [& {:keys [data component-id container-id
                            config config-panel] :as params}]

  ;(log/info "base-chart" params)

  (let [id                (r/atom nil)
        not-configurable? (nil? config-panel)
        d                 (h/resolve-value data)
        c                 (config component-id d)]

    ;(log/info "base-chart (let)"
    ;  component-id container-id
    ;  "//" data "//" @d
    ;  "//" not-configurable?)

    (fn []
      (when (nil? @id)
        ;(log/info "initializing" component-id)
        (reset! id component-id)
        (ui-utils/init-container-locals @id c)
        (ui-utils/dispatch-local @id [:container] container-id))

      [:div.base-chart {:style {:width "100%" :height "100%"}}
       (if not-configurable?
         (reduce into [component-panel] (seq params))

         (reduce into [configurable-component-panel] (seq params)))])))



