(ns bh.ui-component.atom.diagram.editable-digraph
  (:require [bh.ui-component.atom.diagram.diagram.dagre-support :as dagre]
            [bh.ui-component.molecule.composite.util.node-config-ui :as config]
            [bh.ui-component.molecule.composite.util.ui]
            [bh.ui-component.utils.helpers :as h]
            [clojure.set :as set]
            [re-com.core :as rc]
            [reagent.core :as r]
            [taoensso.timbre :as log]
            ["react" :as react]
            ["react-flow-renderer" :refer (ReactFlowProvider MiniMap Controls
                                            Handle MarkerType
                                            Background
                                            applyNodeChanges
                                            applyEdgeChanges
                                            useNodesState
                                            useEdgesState) :default ReactFlow]))


(log/info "bh.ui-component.atom.diagram.editable-digraph")


(declare node)


(def sample-data
  {:nodes [{:id       ":ui/targets"
            ;:type     ":ui/component"
            :data     {:label   ":ui/targets"
                       :inputs  []
                       :outputs []}
            :position {:x 0 :y 100}}
           {:id       ":topic/target-data"
            ;:type     ":source/remote"
            :data     {:label   ":topic/target-data"
                       :inputs  []
                       :outputs []}
            :position {:x 100 :y 0}}
           {:id       ":topic/selected-targets"
            ;:type     ":source/local"
            :data     {:label   ":topic/selected-targets"
                       :inputs  []
                       :outputs []}
            :position {:x 0 :y 200}}]

   :edges [
           {:id        "target-data->targets"
            :source    ":topic/target-data" :target ":ui/targets"
            :style     {:strokeWidth 2 :stroke :orange}
            :markerEnd {:type (.-Arrow MarkerType)}}        ;:type (.-ArrowClosed MarkerType)}}
           {:id        "targets->selected-targets"
            :source    ":ui/targets" :target ":topic/selected-targets"
            :style     {:strokeWidth 2 :stroke :blue}
            :markerEnd {:type (.-ArrowClosed MarkerType)}}]})
(def sample-data-3
  {:nodes [{:id "1 " :type "input" :data {:label "input"} :position {:x 0 :y 0}}
           {:id "2 " :data {:label "node 2 "} :position {:x 0 :y 0}}
           {:id "2a " :data {:label "node 2a "} :position {:x 0 :y 0}}
           {:id "2b " :data {:label "node 2b "} :position {:x 0 :y 0}}
           {:id "2c " :data {:label "node 2c "} :position {:x 0 :y 0}}
           {:id "2d " :data {:label "node 2d "} :position {:x 0 :y 0}}
           {:id "3 " :data {:label "node 3 "} :position {:x 0 :y 0}}
           {:id "4 " :data {:label "node 4 "} :position {:x 0 :y 0}}
           {:id "5 " :data {:label "node 5 "} :position {:x 0 :y 0}}
           {:id "6 " :type "output" :data {:label "output"} :position {:x 0 :y 0}}
           {:id "7 " :type "output" :data {:label "output"} :position {:x 0 :y 0}}]

   :edges [{:id "e12" :source "1 " :target "2 " :type "smoothstep" :animated true}
           {:id "e13" :source "1 " :target "3 " :type "smoothstep" :animated true}
           {:id "e22a" :source "2 " :target "2a " :type "smoothstep" :animated true}
           {:id "e22b" :source "2 " :target "2b " :type "smoothstep" :animated true}
           {:id "e22c" :source "2 " :target "2c " :type "smoothstep" :animated true}
           {:id "e2c2d" :source "2c " :target "2d " :type "smoothstep" :animated true}
           {:id "e45" :source "4 " :target "5 " :type "smoothstep" :animated true}
           {:id "e56" :source "5 " :target "6 " :type "smoothstep" :animated true}
           {:id "e57" :source "5 " :target "7 " :type "smoothstep" :animated true}]})


(def source-code '[])


(def handle-style {:width "8px" :height "8px" :borderRadius "50%"})
(def default-node-style {:padding      "3px" :max-width "180px"
                         :borderRadius "5px" :margin :auto
                         :background   :white :color :black})
(def node-style {:ui/component  {:background :green :color :white}
                 :source/remote {:background :orange :color :black}
                 :source/local  {:background :blue :color :white}
                 :source/fn     {:background :pink :color :black}})


(defn- source-panel [])


;; region ; adding handles to nodes in the digraph

(defn- input-handles
  "
  NOTE: the inputs (values in the hash-map) are STRINGS!
  "
  [label inputs position]
  [:<>
   (doall
     (->> inputs
       (map-indexed (fn [idx [target ports]]
                      (let [[source-port target-port] ports]
                        ;(log/info "input-handle" label target-port)
                        [:> Handle {:id    target-port :type "target" :position position
                                    :style (merge handle-style {:left (+ 20 (* 10 idx))})}])))
       (into [:<>])))])


(defn- output-handles
  "
  NOTE: the inputs (values in the hash-map) are STRINGS!
  "
  [label outputs position]
  [:<>
   (doall
     (->> outputs
       (map-indexed (fn [idx [target ports]]
                      (let [[source-port target-port] ports]
                        ;(log/info "output-handle" label source-port)
                        [:> Handle {:id    source-port :type "source" :position position
                                    :style (merge handle-style {:right (+ 20 (* 10 idx))})}])))
       (into [:<>])))])


(defn- apply-handles [label inputs outputs input-position output-position]
  (let [i        (->> inputs
                   (map (fn [[k [s d]]] [k s d]))
                   (into #{}))
        o        (->> outputs
                   (map (fn [[k [s d]]] [k s d]))
                   (into #{}))
        in-out   (set/intersection i o)
        in-only  (set/difference (set/difference i o) in-out)
        out-only (set/difference (set/difference o i) in-out)]

    (input-handles label in-out input-position)
    (input-handles label in-only input-position)
    (output-handles label out-only output-position)))

;; endregion


(def color-black "#000000")
(def color-white "#ffffff")


;; region ; digraph drag-and-drop support

(defn- on-drag-start [node-type event]
  (.setData (.-dataTransfer event) "editable-flow" node-type)
  (set! (.-effectAllowed (.-dataTransfer event)) "move"))


(defn- on-drag-over [event]
  (.preventDefault event)
  (set! (.-dropEffect (.-dataTransfer event)) "move"))


(defn- on-drop [component-id set-nodes-fn wrapper event]
  (.preventDefault event)
  (let [node-type       (.getData (.-dataTransfer event) "editable-flow")
        x               (.-clientX event)
        y               (.-clientY event)
        reactFlowBounds (.getBoundingClientRect @wrapper)]

    ;(log/info "on-drop" node-type
    ;"//" @wrapper)
    ;"//" (.-current @wrapper)
    ;"//" (.getBoundingClientRect @wrapper))
    ;"//" (js->clj reactFlowBounds)

    (when (not= node-type "undefined")
      (let [new-id   (str node-type "-new")
            new-node {:id       new-id
                      :type     node-type
                      :data     {:label   new-id
                                 :inputs  []
                                 :outputs []}
                      :position {:x (- x (.-left reactFlowBounds))
                                 :y (- y (.-top reactFlowBounds))}}]
        ; TODO: need to trigger a "get back into CLJS and SAVE" operation
        (set-nodes-fn (fn [nds] (.concat nds (clj->js new-node))))))))


(defn- make-draggable-node [[k {:keys [label type color text-color]} :as node]]
  ;(log/info "make-draggable-node" label type "//" node)
  ^{:key label} [:div.draggable
                 {:style       {:width           "150px" :height "50px"
                                :margin-bottom   "5px"
                                :display         :flex
                                :justify-content :center
                                :align-items     :center
                                :cursor          :grab
                                :border-radius   "3px" :padding "2px"
                                :background      color :color text-color}
                  :onDragStart #(on-drag-start type %)
                  :draggable   true}
                 label])

;; endregion


(defn- details-panel [components component-id node]
  (let [details (get node "data")]

    ;(log/info "detail-panel" node "//" details)

    [config/make-config-panel details]))


(defn- tool-panel [open-details? components component-id tool-types]
  ;(log/info "tool-panel" @open-details? "//" component-id "//" tool-types)

  [:div#tool-panel {:display         :flex
                    :flex-direction  :column
                    :justify-content :center
                    :align-items     :center
                    :style           {:width         "20%" :height "100%"
                                      :border-radius "5px" :padding "15px 10px"
                                      :background    :white :box-shadow "5px 5px 5px #888888"}}
   [rc/v-box :src (rc/at)
    :gap "2px"
    :children [[rc/v-box :src (rc/at)
                :gap "2px"
                :justify :center
                :align :center
                :children [(doall
                             (map make-draggable-node tool-types))]]
               [rc/line :size "2px"]
               [:div {:style {:width "20%" :height "100%"}}
                [config/make-config-panel @open-details?]]]]])


(defn- flow* [& {:keys [component-id nodes edges
                        node-types edge-types
                        minimap-styles
                        on-change-nodes on-change-edges on-drop on-drag-over
                        zoom-on-scroll preventScrolling connectFn] :as params}]

  ;(log/info "flow-star (params)" params "// (edge-types)" edge-types)

  (let [params (apply merge {:nodes               nodes
                             :edges               edges
                             :onNodesChange       on-change-nodes
                             :onEdgesChange       on-change-edges
                             :zoomOnScroll        (or zoom-on-scroll false)
                             :preventScrolling    (or preventScrolling false)
                             :onConnect           (or connectFn #())
                             :fitView             true
                             :attributionPosition "bottom-left"
                             :onDrop              (or on-drop #())
                             :onDragOver          (or on-drag-over #())}
                 (when node-types {:node-types node-types})
                 (when edge-types {:edge-types edge-types}))]

    ;(log/info "flow-star (local-params)" local-params)

    [:> ReactFlow params
     [:> MiniMap (if minimap-styles minimap-styles {})]
     [:> Background]
     [:> Controls]]))


(defn- editable-flow [& {:keys [component-id
                                nodes edges
                                node-types edge-types
                                minimap-styles on-drop on-drag-over
                                zoom-on-scroll preventScrolling connectFn
                                force-layout?] :as params}]

  ;(log/info "editable-flow (params)" params)

  (let [{n :nodes e :edges} (if force-layout?
                              (dagre/build-layout nodes edges)
                              {:nodes nodes :edges edges})
        [ns set-nodes on-change-nodes] (useNodesState (clj->js n))
        [es set-edges on-change-edges] (useEdgesState (clj->js e))
        !wrapper (clojure.core/atom nil)]


    ;(log/info "editable-flow"
    ;  "//" (js->clj node-types))
    ;  "//" ns
    ;  "//" nodes)
    ;  "//" set-nodes
    ;  "//" on-change-nodes)

    [:div#wrapper {:style {:width "800px" :height "700px"}
                   :ref   (fn [el]
                            (reset! !wrapper el))}
     [flow*
      :component-id component-id
      :nodes ns :edges es
      :on-change-nodes on-change-nodes
      :on-change-edges on-change-edges
      :node-types node-types
      :edge-types edge-types
      :minimap-styles minimap-styles
      :connectFn connectFn
      :zoom-on-scroll zoom-on-scroll
      :preventScrolling preventScrolling
      :on-drop (partial on-drop component-id set-nodes !wrapper)
      :on-drag-over on-drag-over]]))


(defn component [& {:keys [data
                           node-types edge-types
                           minimap-styles
                           tool-types
                           connectFn zoom-on-scroll preventScrolling
                           component-id container-id
                           force-layout?]}]

  (let [d             (h/resolve-value data)
        open-details? (r/atom {})
        n-types       (->> node-types
                        (map (fn [[k v]]
                               {k (partial v open-details?)}))
                        (into {})
                        (clj->js))]

    ;(log/info "component (DIGRAPH)" "//" data "//" @d "// node-types" node-types "// n-types" (js->clj n-types))

    (fn []
      [rc/h-box :src (rc/at)
       :gap "10px"
       :children [[tool-panel open-details? (:components @d) component-id tool-types]
                  [:f> editable-flow
                   :component-id component-id
                   :nodes (:nodes @d)
                   :edges (:edges @d)
                   :node-types n-types
                   :edge-types edge-types
                   :on-drop on-drop
                   :on-drag-over on-drag-over
                   :minimap-styles (or minimap-styles {})
                   :connectFn connectFn
                   :zoom-on-scroll zoom-on-scroll
                   :preventScrolling preventScrolling
                   :force-layout? force-layout?]]])))




(comment
  (:nodes @sample-data)
  (swap! sample-data assoc :nodes (conj (:nodes @sample-data)
                                    {:id "dummy-node" :position {:x 0 :y 0}}))

  (def node-types {":ui/component"  (partial bh.ui-component.molecule.composite.util.ui/custom-node :ui/component)
                   ":source/remote" (partial bh.ui-component.molecule.composite.util.ui/custom-node :source/remote)
                   ":source/local"  (partial bh.ui-component.molecule.composite.util.ui/custom-node :source/local)
                   ":source/fn"     (partial bh.ui-component.molecule.composite.util.ui/custom-node :source/fn)})
  (def open-details? (r/atom ""))

  (defn- dummy [a b c d]
    (+ a b c d))

  ((partial (partial dummy 1 1) 1 1))
  (->> node-types
    (map (fn [[k v]]
           {k (partial v open-details?)}))
    (into {})
    (clj->js))

  ())

