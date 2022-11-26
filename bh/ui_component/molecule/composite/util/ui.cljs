(ns bh.ui-component.molecule.composite.util.ui
  (:require [bh.ui-component.atom.diagram.diagram.dagre-support :as dagre]
            [bh.ui-component.molecule.composite.util.signals :as sig]
            [bh.ui-component.utils.locals :as ul]
            [reagent.core :as r]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [taoensso.timbre :as log]
            ["react-flow-renderer" :refer (ReactFlowProvider Controls Handle Background) :default ReactFlow]))


(log/info "bh.ui-component.molecule.composite.util.ui")


(def handle-style {:width "8px" :height "8px" :borderRadius "50%"})
(def default-node-style {:padding      "3px" :max-width "180px"
                         :borderRadius "5px" :margin :auto
                         :background   :white :color :black})
(def node-style {:ui/component  {:background :green :color :white}
                 :source/remote {:background :orange :color :black}
                 :source/local  {:background :blue :color :white}
                 :source/fn     {:background :pink :color :black}})


(defn input-output-handles
  "

  NOTE: the inputs (values in the hash-map) are STRINGS!
  "

  [label inputs outputs]
  [:<>
   ; add the input handles
   (doall
     (->> inputs
       (map-indexed (fn [idx [target ports]]
                      (let [[source-port target-port] ports]
                        ;(log/info "input-handle" label "/" target-port "///" target "/" source-port)
                        [:> Handle {:id    target-port :type "target" :position "top"
                                    :style (merge handle-style {:left (+ 20 (* 10 idx))})}])))
       (into [:<>])))

   ; add the output handles
   (doall
     (->> outputs
       (map-indexed (fn [idx [target ports]]
                      (let [[source-port target-port] ports]
                        ;(log/info "output-handle" label "/" source-port "///" target "/" target-port)
                        [:> Handle {:id    source-port :type "source" :position "bottom"
                                    :style (merge handle-style {:left (+ 20 (* 10 idx))})}])))
       (into [:<>])))])


(defn- open-details [open-details? node]
  (reset! open-details? (js->clj node)))
  ;(log/info "open-details" @open-details?))


(defn custom-node
  "build a custom node for the flow diagram, this time for :ui/component, so
  green, since this is a 'view', and one Handle for each input (along the top)
  and output (along the bottom)
  "
  [node-type open-details? d & extras?]
  (let [data    (js->clj d)
        label   (get-in data ["data" "label"])
        inputs  (get-in data ["data" "inputs"])
        outputs (get-in data ["data" "outputs"])
        style   (merge default-node-style (node-type node-style))]

    ;(log/info "custom-node" label data "///" inputs "///" outputs "//" extras?)

    (r/as-element
      [:div {:style style :on-click #(open-details open-details? d)}
       [:h5 {:style (merge {:textAlign :center} style)} label]
       (input-output-handles label inputs outputs)])))


(defn create-flow-node
  "convert the nodes, currently organized by Loom (https://github.com/aysylu/loom), into
  the format needed by react-flow (https://reactflow.dev)
  "
  [configuration node-id]
  (let [node-type (get-in configuration [:components node-id :type])]
    ;(log/info "node" node-id node-type)
    {:id       (str node-id)
     :type     (str node-type)
     :data     {:label   (str node-id)
                :node-type (str node-type)
                :inputs  (->>
                           (get-in configuration [:denorm node-id :inputs])
                           (map (fn [[k v]]
                                  (let [[sp tp] v]
                                    {(str k) [(str sp) (str tp)]})))
                           (into {}))
                :outputs (->>
                           (get-in configuration [:denorm node-id :outputs])
                           (map (fn [[k v]]
                                  (let [[sp tp] v]
                                    {(str k) [(str sp) (str tp)]})))
                           (into {}))}
     :position {:x 0 :y 0}}))


(defn create-flow-edge
  "convert the edges, currently organized by Loom (https://github.com/aysylu/loom), into
  the format needed by react-flow (https://reactflow.dev)
  "
  [configuration idx [node-id target-id :as edge]]
  (let [[source-handle target-handle] (get-in configuration [:denorm node-id :outputs target-id])]

    ;(log/info "flow-edge" idx "/" node-id "/" source-handle "///" target-id "/" target-handle)

    {:id            (str idx)
     :source        (str node-id)
     :sourceHandle  (str source-handle)
     :target        (str target-id)
     :targetHandle  (str target-handle)
     :label         (str source-handle "->" target-handle)
     :style         {:strokeWidth 1 :stroke :black}
     :arrowHeadType "arrowclosed"
     :animated      false}))


(defn compute-edges
  "pull out just the relevant information from the configuration, so it can be passed into Loom and
  the interconnected digraph can be built
  "
  [configuration]
  (->> configuration
    :links
    (mapcat (fn [[entity links]]
              (mapcat (fn [[source-port targets]]
                        (map (fn [[target target-port]]
                               [entity target])
                          targets))
                links)))
    (into [])))


(defn make-flow
  "take the Loom graph and turn it into what react-flow needs to draw it onto the display
  "
  [configuration]
  (let [flow {:nodes (map #(create-flow-node configuration %) (:nodes configuration))
              :edges (map-indexed (fn [idx node]
                                    (create-flow-edge configuration idx node))
                       (:edges configuration))}]
    (dagre/build-layout flow)))


(defn prep-environment [configuration component-id registry]
  ; 1. remote subscriptions (including the remote call)
  ;
  ; [SIDE EFFECT]
  (sig/process-components configuration :source/remote registry component-id)

  ; 2. build the subscription for the "container" which provide the basis for the
  ;     subscriptions for the "locals"
  ;
  ; [SIDE EFFECT]
  (ul/create-container-sub component-id)
  (ul/create-container-local-sub component-id [:blackboard] {})

  ; 3. add blackboard data to the app-db and build local subscriptions/events against the blackboard
  ;
  ; [SIDE EFFECT]
  (sig/process-components configuration :source/local registry component-id)

  ; 4. local functions (to build subscriptions against the blackboard or remotes)
  ;
  ; [SIDE EFFECT]
  (sig/process-components configuration :source/fn registry component-id))

