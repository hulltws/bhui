(ns bh.ui-component.molecule.composite.util.node-config-ui
  (:require [taoensso.timbre :as log]
            [bh.ui-component.utils.helpers :as h]
            [bh.ui-component.utils.locals :as l]))


(log/info "bh.ui-component.molecule.composite.util.node-config-ui")


;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;
;
; build the correct data-entry control for each type
;
;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;
(defmulti make-config-item (fn [{:keys [type]}] type))


(defmethod make-config-item :string [{:keys [type name]}]
  [:div (str ":string - " type " - " name)])


(defmethod make-config-item :id [{:keys [type name]}]
  [:div (str ":id - " type " - " name)])


(defmethod make-config-item :port [{:keys [type name]}]
  [:div (str ":port - " type " - " name)])


(defmethod make-config-item :choices [{:keys [type name]}]
  [:div (str ":choices - " type " - " name)])


;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;
;
; build the complete panel for each type
;
;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;
(defmulti make-config-panel* (fn [type node] type))


(defmethod make-config-panel* :ui/component [type {:strs [id] :as node}]
  ;(log/info "make-config-panel* :ui/component" type id)
  [:div.ui-component (str type " - " id)])


(defmethod make-config-panel* :source/remote [type {:strs [id] :as node}]
  ;(log/info "make-config-panel* :source/remote" type id)
  [:div.source-remote (str type " - " id)])


(defmethod make-config-panel* :source/local [type {:strs [id] :as node}]
  ;(log/info "make-config-panel* :source/local" type id)
  [:div.source-local (str type " - " id)])


(defmethod make-config-panel* :source/fn [type {:strs [id] :as node}]
  ;(log/info "make-config-panel* :source/fn" type id)
  [:div.source-fn (str type " - " id)])


(defmethod make-config-panel* :default [type {:strs [id] :as node}]
  ;(log/info "make-config-panel* :default" type "//" id "//" node)
  [:div])


(defn make-config-panel [node]
  (let [node-type (get node "type")
        kw-node-type (get {":ui/component" :ui/component
                           ":source/remote" :source/remote
                           ":source/local" :source/local
                           ":source/fn" :source/fn}
                       node-type)]

    ;(log/info "make-config-panel" node "//" node-type "//" kw-node-type)

    (make-config-panel* kw-node-type node)))



(comment
  (do
    (def component-id :widget-grid-demo.grid-widget)
    (def item :topic/target-data)
    (def components   @(l/subscribe-local component-id [:blackboard :defs :source :components]))
    (def details      ((h/string->keyword item) components))
    (def detail-types (:type details)))


  (map (fn [{:keys [type name]}] {:t type :n name}) details)

  (make-config-panel details)

  (defmulti dummy (fn [type] (keyword (get type "type"))))
  (defmethod dummy :one [_] 1)
  (defmethod dummy :two [_] 2)

  (dummy {"type" ":one"})


  (get {"type" ":one"} "type")

  (let [{:strs [type]} {"type" ":one"}]
    (keyword type))
  ())




