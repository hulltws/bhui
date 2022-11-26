(ns bh.ui-component.molecule.composite.util.digraph
  "Expand on the configuration, computing denormalized data, the Loom digraph, etc."
  (:require [day8.re-frame.tracing :refer-macros [fn-traced]]
            [loom.graph :as lg]
            ["dagre" :as dagre]
            ["graphlib" :as graphlib]
            ["react-flow-renderer" :refer (ReactFlowProvider Controls Handle Background) :default ReactFlow]))


(defn expand-components [data registry]
  (let [components (:components data)]
    (->> data
      :components
      (map (fn [[id meta-data]]
             {id (assoc meta-data
                   :ports
                   (condp = (:type meta-data)
                     :ui/component (->> components id :name registry :ports)
                     :source/remote {:port/pub-sub :data}
                     :source/local {:port/pub-sub :data}
                     :source/fn (:ports meta-data)))}))
      (assoc data :components))))


(defn get-predecessor-name [links graph source target]
  ;(log/info "pred" source target "//" graph)
  (->> links
    (filter (fn [[s _]]
              (and (contains? (lg/predecessors* graph source) s)
                (= s target))))
    vals
    first
    keys
    first))


(defn get-successor-name [links graph source target]
  (->> links
    source
    (filter (fn [[s _]]
              (contains? (lg/successors* graph source) target)))
    vals
    first
    vals
    first))


(defn get-inputs
  "get all the inputs to the given node (these are 'predecessors')

  we grab the node's predecessors, and format the data correctly:

  {<source> [<node's-port> <source's-port>]
   <source> [<node's-port> <source's-port>]}


  WORK-IN-PROGRESS
  "
  [links graph node]
  (->> node
    (lg/predecessors* graph)
    (map (fn [p]
           ; 1. grab the target meta-data for each source
           (apply merge
             (map (fn [[source-port targets]]
                    (let [target-port (get targets node)]
                      {p [source-port target-port]}))
               (get links p)))))
    (into {})))


(defn get-outputs [links node]
  "get all the outputs of the given node

  these are given directly by the links, but need reformatting from:

  {<source's-port {<target> <target's-port>
                   <target> <target's-port>}}

  to:

  {<target> [<node's-port> <target's-port>]
   <target> [<node's-port> <target's-port>]}


  WORK-IN-PROGRESS
  "
  (->> links
    node
    (map (fn [[node-port target-meta]]
           (apply merge
             (map (fn [[target target-port]]
                    {target [node-port target-port]})
               target-meta))))
    (apply merge)))


(defn denorm-components
  "denormalize the links between components by mixing in additional information bout the
  ports at both ends of the inter-connection:

  {<node> {:inputs  {<source> [<node's-port> <source's-port>]
                     <source> [<node's-port> <source's-port>]}
           :outputs {<target> [<node's-port> <target's-port>]
                     <target> [<node's-port> <target's-port>]}
           :params  {<source> [<node's-port> <source's-port>]
                     <source> [<node's-port> <source's-port>]
                     <target> [<node's-port> <target's-port>]
                     <target> [<node's-port> <target's-port>]}}

  WORK-IN-PROGRESS
  "
  [graph links nodes]
  (->> nodes
    (map (fn [node]
           {node
            {:inputs  (get-inputs links graph node)
             :outputs (get-outputs links node)
             :params  {}}}))
    (into {})))

