(ns bh.ui-component.organism.ui-grid
  (:require [bh.ui-component.atom.layout.responsive-grid :as grid]
            [bh.ui-component.utils.helpers :as h]
            [reagent.ratom]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.organism.ui-grid")


(defn- make-widget [[id title content bk-color txt-color]]
  ;(log/info "make-widget" id "//" title)

  [:div.widget-parent {:key   id
                       :style {:width "100%" :height "100%"}}
   [:div.grid-toolbar.title-wrapper.move-cursor
    [:div {:style {:background-color bk-color
                   :color            txt-color
                   :padding          "5px"
                   :font-weight      :bold
                   :font-size        "1.1em"}}
     title]]
   [:div.widget.widget-content
    {:style         {:width       "100%"
                     :height      "90%"
                     :cursor      :default
                     :align-items :stretch
                     :display     :flex}
     :on-mouse-down #(.stopPropagation %)}
    content]])


(defn- update-layout-sub [layout updated-layout]
  ;(log/info "update-layout-sub" layout "//" updated-layout)
  (h/handle-change-path layout [] updated-layout))


(defn- update-layout-ratom [layout updated-layout]
  ;(log/info "update-layout-ratom" layout "//" updated-layout)
  (reset! layout updated-layout))


(defn- update-layout [layout updated-layout]
  ;(log/info "update-layout" layout "//" updated-layout)
  ;
  ;(log/info "update-layout" (keyword? layout)
  ;  "//" (coll? layout)
  ;  "//" (seq layout)
  ;  "//" (every? keyword? layout)
  ;  "////" (instance? reagent.ratom.RAtom layout)
  ;  "//" (instance? Atom layout))

  (cond
    (keyword? layout) (update-layout-sub layout updated-layout)

    (and (coll? layout)
      (seq layout)
      (every? keyword? layout)) (update-layout-sub layout updated-layout)

    (or (instance? reagent.ratom.RAtom layout)
      ;(instance? reagent.ratom.Reaction layout)
      (instance? Atom layout)) (update-layout-ratom layout updated-layout)

    :else ()))


(defn on-layout-change [layout new-layout]
  ;; note the need to convert the callbacks from js objects
  (let [n-l (js->clj new-layout :keywordize-keys true)
        fst (first n-l)]

    ;(log/info "on-layout-change" @widgets "//" new-layout)

    (when (and
            (seq n-l)
            (<= 1 (count n-l))
            (not= (:i fst) "null"))
      (let [cooked (map #(zipmap '(:i :x :y :w :h) %)
                     (map (juxt :i :x :y :w :h) n-l))]
        ;(log/info "on-layout-change (cooked)" cooked
        ;  "//" (zipmap (map :i cooked) cooked))
        (update-layout layout cooked)))))


(defn component [& {:keys [widgets layout container-id]}]

  (let [r-widgets (h/resolve-value widgets)
        r-layout  (h/resolve-value layout)]

    ;(log/info "component (resolve)" container-id
    ;  "//" @r-widgets
    ;  "//" @r-layout)

    (fn []
      [grid/grid
       :id container-id
       :children (doall (map make-widget @r-widgets))
       :cols 20
       :layoutFn (partial on-layout-change layout)
       :layout @r-layout])))