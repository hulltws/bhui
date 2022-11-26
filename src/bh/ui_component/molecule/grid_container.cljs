(ns bh.ui-component.molecule.grid-container
  (:require [bh.ui-component.atom.layout.responsive-grid :as grid]
            [bh.ui-component.molecule.composite :as composite]
            [bh.ui-component.molecule.composite.util.digraph :as dig]
            [bh.ui-component.molecule.composite.util.signals :as sig]
            [bh.ui-component.molecule.composite.util.ui :as ui]
            [bh.ui-component.utils :as ui-utils]
            [bh.ui-component.utils.locals :as locals]
            [bh.ui-component.atom.re-com.configure-toggle :as ct]
            [loom.graph :as lg]
            [re-com.core :as rc]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.molecule.grid-container")


(defn- config
  "set up the local config keys, specifically we want the :layout key, so we can
  track updates to the layout should the user drag/resize any of the internal
  components.

  the component-panel will subscribe to this 'local' using (locals/subscribe-local ...) and
  dispatch updates (via on-layout-update) using (locals/dispatch-local ...)
  "
  [full-config]
  {:blackboard {:defs {:source full-config
                       :dag    {:open-details ""}}}
   :container  ""
   :layout     (:grid-layout full-config)})


(defn- wrap-component [[id component label]]

  ;(log/info "wrap-component" id "//" label "//" component)

  [:div.widget-parent {:key id}
   [:div.grid-toolbar.title-wrapper.move-cursor
    {:style {:height "1.5em"}}
    (or label (name id))]
   [:div.widget.widget-content
    {:style         {:width       "100%"
                     :height      "90%"
                     :cursor      :default
                     :align-items :stretch
                     :display     :flex}
     :on-mouse-down #(.stopPropagation %)}
    component]])


(defn- on-width-update
  "
  ---

  - width : (number) new width of the container
  - margin : (vector) margin [left? right?]
  - cols : (number) number of columns
  - padding : (vector) padding [left? right?]
  "
  [width margin cols padding]

  (log/info "on-width-update" width "//" margin "//" cols "//" padding)

  ())


(defn- on-layout-change [component-id new-layout all-layouts]
  (let [new-layout*  (js->clj new-layout :keywordize-keys true)
        all-layouts* (js->clj all-layouts :keywordize-keys true)
        fst          (first new-layout*)]

    ;(log/info "on-layout-change" new-layout*
    ;  "//" all-layouts*
    ;  "//" (keys all-layouts*))

    (when (and
            (not (empty? new-layout*))
            (<= 1 (count new-layout*))
            (not= (:i fst) "null"))
      (let [cooked (map #(zipmap '(:i :x :y :w :h :static) %)
                     (map (juxt :i :x :y :w :h :static) new-layout*))]
        ;(log/info "on-layout-change (cooked)" cooked)
        (locals/dispatch-local component-id [:layout] cooked)))))


(defn- toggle-editable [orig-value]
  (map #(assoc % :static (-> % :static not)) orig-value))


(defn- component-panel [& {:keys [configuration component-id resizable] :as params}]
  ;(log/info "component-panel (params)" params)

  ;(log/info "component-panel" component-id
  ;  "//" (keys configuration)
  ;  "// dummy-layout" dummy-layout
  ;  "// :components" (:components configuration)
  ;  "// process-components" (into {}
  ;                            (sig/process-components
  ;                              configuration :ui/component
  ;                              @(re-frame/subscribe [:meta-data-registry]) component-id)))

  (let [layout              (locals/subscribe-local component-id [:layout])
        component-lookup    (into {}
                              (sig/process-components
                                configuration :ui/component
                                @(re-frame/subscribe [:meta-data-registry]) component-id))

        ; 1. build UI components (with subscription/event signals against the blackboard or remotes)
        composed-ui         (map wrap-component component-lookup)
        open?               (r/atom false)]

    (fn []
      ;(log/info "component-panel INNER" component-id
      ;  "//" @layout
      ;  "//" composed-ui)

      ; 5. return the composed component layout!
      [rc/v-box :src (rc/at)
       :gap "2px"
       :children [(when resizable [ct/configure-toggle open? #(locals/apply-local component-id
                                                                [:layout] toggle-editable)])
                  [:div.grid-container {:style {:width "100%" :height "100%"}}
                   [grid/grid
                    :id component-id
                    :class "layout"
                    :children composed-ui
                    :layout @layout
                    :cols 20
                    :width 1200
                    :rowHeight 25
                    :layoutFn #(on-layout-change component-id %1 %2)
                    :widthFn #(on-width-update %1 %2 %3 %4)]]]])))


(defn component [& {:keys [data component-id container-id resizable tools] :as params}]

  ;(log/info "component" data "//" component-id "//" container-id)
  ;(log/info "component (params)" params)

  (let [id            (r/atom nil)
        configuration @data
        graph         (apply lg/digraph (ui/compute-edges configuration))
        comp-or-dag?  (r/atom :component)
        partial-config   (assoc configuration
                           :denorm (dig/denorm-components graph (:links configuration) (lg/nodes graph))
                           :nodes (-> configuration
                                    :components
                                    keys
                                    set)
                           :edges (into [] (lg/edges graph)))
        full-config (assoc partial-config :graph graph)]

    (fn []
      (when (nil? @id)
        (reset! id component-id)
        (ui-utils/init-container-locals @id (config partial-config))
        (ui-utils/dispatch-local @id [:container] container-id)
        (ui/prep-environment partial-config @id @(re-frame/subscribe [:meta-data-registry])))

      (let [buttons [{:id :component :tooltip "Widget view" :label [:i {:class "zmdi zmdi-view-compact"}]}
                     {:id :dag :tooltip "Event model view" :label [:i {:class "zmdi zmdi-share"}]}
                     {:id :definition :tooltip "Text view"  :label [:i {:class "zmdi zmdi-format-subject"}]}]]

        [:div.box {:style {:width "100%" :height "100%" :background "#faeee8"}}
         [rc/v-box :src (rc/at)
          ;:justify :start
          :width "100%"
          :height "100%"
          :gap "5px"
          :children [(when tools [rc/h-box :src (rc/at)
                                  :justify :end
                                  :children [[rc/horizontal-bar-tabs
                                              :model comp-or-dag?
                                              :tabs buttons
                                              :on-change #(reset! comp-or-dag? %)]]])
                     (condp = @comp-or-dag?
                       :dag [composite/dag-panel
                             :configuration full-config
                             :component-id @id
                             :container-id container-id]
                       :component [component-panel
                                   :configuration full-config
                                   :component-id @id
                                   :container-id container-id
                                   :resizable resizable]
                       :definition [composite/definition-panel
                                    :configuration configuration]
                       :default [rc/alert-box :src (rc/at)
                                 :alert-type :warning
                                 :body "There is a problem with this component."])]]]))))




(comment
  (def component-id :widget-grid-demo.grid-widget)


  (locals/subscribe-local component-id [:layout])
  (locals/apply-local component-id [:layout] toggle-editable)


  (def orig-value @(re-frame/subscribe [:widget-grid-demo.grid-widget.layout]))
  (toggle-editable orig-value)


  ())


(comment
  (def component-id :widget-grid-demo.grid-widget)
  (def layout (r/atom [{:id 1 :static true}]))
  (def make-editable-style {:md-icon-name "zmdi-wrench"
                            :tooltip      "configure this chart"})
  (def save-editable-style {:md-icon-name "zmdi-lock-outline"
                            :tooltip      "Save the configuration"})

  (reduce conj [rc/md-icon-button] (flatten (seq {:class "button"})))

  (reduce conj [rc/md-icon-button]
    (flatten
      (seq
        (merge {:class    "button"
                :on-click #(locals/apply-local component-id [:layout] toggle-editable)}
          (if (-> @layout first :static) make-editable-style save-editable-style)))))

  ())

