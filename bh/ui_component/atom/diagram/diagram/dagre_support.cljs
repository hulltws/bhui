(ns bh.ui-component.atom.diagram.diagram.dagre-support
  (:require [taoensso.timbre :as log]
            ["dagre" :as dagre]
            ["graphlib" :as graphlib]))


(log/info "bh.ui-component.atom.diagram.diagram.dagre-support")


(defn dagre-graph
  "copy the nodes and edges from Look to dagre, so we can use dagre layout function to put them
  onto the display without drawing over each other
  "
  ([nodes edges]
   (let [dagreGraph (new (.-Graph graphlib))
         nodeWidth  172
         nodeHeight 36]

     (.setDefaultEdgeLabel dagreGraph (clj->js {}))
     (.setGraph dagreGraph (clj->js {:rankdir "tb"}))

     (doall
       (map (fn [element]
              (.setNode dagreGraph (:id element)
                (clj->js {:width nodeWidth :height nodeHeight})))
         nodes))

     (doall
       (map (fn [element]
              (.setEdge dagreGraph (:source element) (:target element)))
         edges))

     dagreGraph))

  ([graph]
   (dagre-graph (:nodes graph) (:edges graph))))


(defn build-layout
  "use dagre (see https://reactflow.dev/examples/layouting/) to perform an auto-layout of the nodes,
  which are then connected by the edges.
  "
  ([nodes edges]
   (let [dagreGraph (dagre-graph nodes edges)
         nodeWidth  172
         nodeHeight 36]

     (.layout dagre dagreGraph)

     (let [ret {:nodes (doall
                         (map (fn [element]
                                (let [dagreNode (.node dagreGraph (clj->js (:id element)))
                                      x         (- (.-x dagreNode) (/ nodeWidth 2))
                                      y         (- (.-y dagreNode) (/ nodeHeight 2))]
                                  ;(log/info "pos" (:id element) x y "//" (.-x dagreNode) (.-y dagreNode))
                                  (assoc element :position {:x x :y y}
                                                 :targetPosition "top"
                                                 :sourcePosition "bottom")))
                           nodes))
                :edges edges}]
                ;(doall
                ;  (map (fn [element] element)
                ;    edges))}]
       ;(log/info "build-layout" ret)
       ret)))

  ([graph]
   (build-layout (:nodes graph) (:edges graph))))


(defn dump-dagre [dagreGraph]
  (doall
    (map (fn [n]
           (println "node" (js->clj n)))
      (.nodes dagreGraph)))
  (doall
    (map (fn [n]
           (println "edge" (js->clj n)))
      (.edges dagreGraph))))


(comment
  (do
    (def graph {:nodes [{:id       ":ui/targets"
                         :type     ":ui/component"
                         :data     {:label   ":ui/targets"
                                    :inputs  []
                                    :outputs []}
                         :position {:x 0 :y 0}}
                        {:id       ":topic/target-data"
                         :type     ":source/remote"
                         :data     {:label   ":topic/target-data"
                                    :inputs  []
                                    :outputs []}
                         :position {:x 0 :y 100}}
                        {:id       ":topic/selected-targets"
                         :type     ":source/local"
                         :data     {:label   ":topic/selected-targets"
                                    :inputs  []
                                    :outputs []}
                         :position {:x 0 :y 200}}]

                :edges [{:id     "target-data->targets"
                         :source ":topic/target-data" :target ":ui/targets"
                         :style  {:strokeWidth 2 :stroke :orange}}
                        {:id     "targets->selected-targets"
                         :source ":ui/targets" :target ":topic/selected-targets"
                         :style  {:strokeWidth 2 :stroke :blue}}]}))


  (dagre-graph graph)
  (build-layout graph)



  ())