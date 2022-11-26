(ns bh.ui-component.atom.diagram.diagram.composite-dag-support
  (:require [bh.ui-component.molecule.composite.util.ui :as ui]
            [taoensso.timbre :as log]
            ["react-flow-renderer" :refer (MarkerType) :default ReactFlow]))


(log/info "bh.ui-component.atom.diagram.diagram.composite-dag-support")


(defn custom-minimap-node-color [color-pallet default-color node]
  ;(log/info "custom-minimap-node-color" node)
  (or (get color-pallet (.-type node)) default-color))


(defn default-custom-node
  "build a custom node for the flow diagram, based on the :type property of the node
  "
  [type open-details? node & extras?]

  ;(log/info "default-custom-node" type "//" @open-details? "//" node "//" extras?)

  (ui/custom-node type open-details? node))


(def color-black "#000000")
(def color-white "#ffffff")

(def node-style {:ui/component  {:background :green :color :white}
                 :source/remote {:background :orange :color :black}
                 :source/local  {:background :blue :color :white}
                 :source/fn     {:background :pink :color :black}})
(def default-color-pallet {":ui/component"  "#00ff00"
                           ":source/remote" "#FFA500"
                           ":source/local"  "#0000ff"
                           ":source/fn"     "#FFC0CB"})
(def default-node-types {":ui/component"  (partial default-custom-node :ui/component)
                         ":source/remote" (partial default-custom-node :source/remote)
                         ":source/local"  (partial default-custom-node :source/local)
                         ":source/fn"     (partial default-custom-node :source/fn)})
(def default-minimap-styles {:nodeStrokeColor  (partial custom-minimap-node-color
                                                 default-color-pallet color-white)
                             :node-color       (partial custom-minimap-node-color
                                                 default-color-pallet color-black)
                             :nodeBorderRadius 5})
(def default-tool-types {:ui/component  {:label ":ui/component" :type :ui/component :color "green" :text-color :white}
                         :source/remote {:label ":source/remote" :type :source/remote :color "orange" :text-color :black}
                         :source/local  {:label ":source/local" :type :source/local :color "blue" :text-color :white}
                         :source/fn     {:label ":source/fn" :type :source/fn :color "pink" :text-color :black}})


(def sample-data
  {:nodes [{:id       ":fn/range",
            :type     ":source/fn",
            :data     {:label   ":fn/range",
                       :inputs  {":topic/coverage-data" [":data" ":data"]},
                       :outputs {":topic/time-range" [":range" ":data"]}},
            :position {:x 0, :y 86}}
           {:id       ":ui/globe",
            :type     ":ui/component",
            :data     {:label   ":ui/globe",
                       :inputs  {":topic/shapes" [":data" ":shapes"], ":topic/current-time" [":data" ":current-time"]},
                       :outputs {}},
            :position {:x 0, :y 602}}
           {:id       ":topic/coverage-data",
            :type     ":source/remote",
            :data     {:label   ":topic/coverage-data",
                       :inputs  {},
                       :outputs {":fn/coverage" [":data" ":coverages"], ":fn/range" [":data" ":data"]}},
            :position {:x 454, :y 0}}
           {:id       ":topic/shapes",
            :type     ":source/local",
            :data     {:label   ":topic/shapes",
                       :inputs  {":fn/coverage" [":shapes" ":data"]},
                       :outputs {":ui/globe" [":data" ":shapes"]}},
            :position {:x 615.5, :y 516}}
           {:id       ":ui/satellites",
            :type     ":ui/component",
            :data     {:label   ":ui/satellites",
                       :inputs  {":topic/satellite-data" [":data" ":data"]},
                       :outputs {":topic/satellite-data" [":data" ":data"], ":topic/selected-satellites" [":selection" ":data"]}},
            :position {:x 333, :y 258}}
           {:id       ":topic/current-time",
            :type     ":source/local",
            :data     {:label   ":topic/current-time",
                       :inputs  {":ui/time-slider" [":value" ":data"]},
                       :outputs {":ui/current-time" [":data" ":value"],
                                 ":ui/time-slider"  [":data" ":value"],
                                 ":ui/globe"        [":data" ":current-time"],
                                 ":fn/coverage"     [":data" ":current-time"]}},
            :position {:x 0, :y 344}}
           {:id       ":fn/coverage",
            :type     ":source/fn",
            :data     {:label   ":fn/coverage",
                       :inputs  {":topic/coverage-data"       [":data" ":coverages"],
                                 ":topic/current-time"        [":data" ":current-time"],
                                 ":topic/selected-targets"    [":data" ":targets"],
                                 ":topic/selected-satellites" [":data" ":satellites"]},
                       :outputs {":topic/shapes" [":shapes" ":data"]}},
            :position {:x 615.5, :y 430}}
           {:id       ":topic/time-range",
            :type     ":source/local",
            :data     {:label   ":topic/time-range",
                       :inputs  {":fn/range" [":range" ":data"]},
                       :outputs {":ui/time-slider" [":data" ":range"]}},
            :position {:x 0, :y 172}}
           {:id       ":ui/time-slider",
            :type     ":ui/component",
            :data     {:label   ":ui/time-slider",
                       :inputs  {":topic/current-time" [":data" ":value"], ":topic/time-range" [":data" ":range"]},
                       :outputs {":topic/current-time" [":value" ":data"]}},
            :position {:x 0, :y 258}}
           {:id       ":topic/target-data",
            :type     ":source/remote",
            :data     {:label   ":topic/target-data",
                       :inputs  {":ui/targets" [":selection" ""]},
                       :outputs {":ui/targets" [":data" ":data"]}},
            :position {:x 666, :y 172}}
           {:id       ":topic/satellite-data",
            :type     ":source/remote",
            :data     {:label   ":topic/satellite-data",
                       :inputs  {":ui/satellites" [":selection" ""]},
                       :outputs {":ui/satellites" [":data" ":data"]}},
            :position {:x 222, :y 344}}
           {:id       ":ui/targets",
            :type     ":ui/component",
            :data     {:label   ":ui/targets",
                       :inputs  {":topic/target-data" [":data" ":data"]},
                       :outputs {":topic/target-data" [":data" ":data"], ":topic/selected-targets" [":selection" ":data"]}},
            :position {:x 666, :y 258}}
           {:id       ":topic/selected-targets",
            :type     ":source/local",
            :data     {:label   ":topic/selected-targets",
                       :inputs  {":ui/targets" [":selection" ":data"]},
                       :outputs {":fn/coverage" [":data" ":targets"]}},
            :position {:x 666, :y 344}}
           {:id       ":topic/selected-satellites",
            :type     ":source/local",
            :data     {:label   ":topic/selected-satellites",
                       :inputs  {":ui/satellites" [":selection" ":data"]},
                       :outputs {":fn/coverage" [":data" ":satellites"]}},
            :position {:x 444, :y 344}}
           {:id       ":ui/current-time",
            :type     ":ui/component",
            :data     {:label ":ui/current-time", :inputs {":topic/current-time" [":data" ":value"]}, :outputs {}},
            :position {:x 60.5, :y 430}}]
   :edges [{:targetHandle ":data" :animated false
            :source       ":fn/range" :style {:strokeWidth 1 :stroke :black}
            :label        ":data" :id "0" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":range" :target ":topic/time-range"}
           {:targetHandle ":coverages" :animated false
            :source       ":topic/coverage-data" :style {:strokeWidth 1 :stroke :black}
            :label        ":coverages" :id "1" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":fn/coverage"}
           {:targetHandle ":data" :animated false
            :source       ":topic/coverage-data" :style {:strokeWidth 1 :stroke :black}
            :label        ":data" :id "2" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":fn/range"}
           {:targetHandle ":shapes" :animated false
            :source       ":topic/shapes" :style {:strokeWidth 1 :stroke :black}
            :label        ":shapes" :id "3" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":ui/globe"}
           {:targetHandle ":data" :animated false
            :source       ":ui/satellites" :style {:strokeWidth 1 :stroke :black}
            :label        ":data" :id "4" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":topic/satellite-data"}
           {:targetHandle ":data" :animated false
            :source       ":ui/satellites" :style {:strokeWidth 1 :stroke :black}
            :label        ":data" :id "5" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":selection" :target ":topic/selected-satellites"}
           {:targetHandle ":value" :animated false
            :source       ":topic/current-time" :style {:strokeWidth 1 :stroke :black}
            :label        ":value" :id "6" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":ui/current-time"}
           {:targetHandle ":value" :animated false
            :source       ":topic/current-time" :style {:strokeWidth 1 :stroke :black}
            :label        ":value" :id "7" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":ui/time-slider"}
           {:targetHandle ":current-time" :animated false
            :source       ":topic/current-time" :style {:strokeWidth 1 :stroke :black}
            :label        ":current-time" :id "8" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":ui/globe"}
           {:targetHandle ":current-time" :animated false
            :source       ":topic/current-time" :style {:strokeWidth 1 :stroke :black}
            :label        ":current-time" :id "9" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":fn/coverage"}
           {:targetHandle ":data" :animated false
            :source       ":fn/coverage" :style {:strokeWidth 1 :stroke :black}
            :label        ":data" :id "10" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":shapes" :target ":topic/shapes"}
           {:targetHandle ":range" :animated false
            :source       ":topic/time-range" :style {:strokeWidth 1 :stroke :black}
            :label        ":range" :id "11" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":ui/time-slider"}
           {:targetHandle ":data" :animated false
            :source       ":ui/time-slider" :style {:strokeWidth 1 :stroke :black}
            :label        ":data" :id "12" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":value" :target ":topic/current-time"}
           {:targetHandle ":data" :animated false
            :source       ":topic/target-data" :style {:strokeWidth 1 :stroke :black}
            :label        ":data" :id "13" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":ui/targets"}
           {:targetHandle ":data" :animated false
            :source       ":topic/satellite-data" :style {:strokeWidth 1 :stroke :black}
            :label        ":data" :id "14" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":ui/satellites"}
           {:targetHandle ":data" :animated false
            :source       ":ui/targets" :style {:strokeWidth 1 :stroke :black}
            :label        ":data" :id "15" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":topic/target-data"}
           {:targetHandle ":data" :animated false
            :source       ":ui/targets" :style {:strokeWidth 1 :stroke :black}
            :label        ":data" :id "16" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":selection" :target ":topic/selected-targets"}
           {:targetHandle ":targets" :animated false
            :source       ":topic/selected-targets" :style {:strokeWidth 1 :stroke :black}
            :label        ":targets" :id "17" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":fn/coverage"}
           {:targetHandle ":satellites" :animated false
            :source       ":topic/selected-satellites" :style {:strokeWidth 1 :stroke :black}
            :label        ":satellites" :id "18" :markerEnd {:type (.-ArrowClosed MarkerType)}
            :sourceHandle ":data" :target ":fn/coverage"}]})


