(ns bh.ui-component.molecule.composite
  ; TODO: we can refactor all function into grid-widget
  "provides a 'container' to hold and organize other atoms and molecules
components have 'ports' which define their inputs and outputs:

    you SUBSCRIBE with a :port/sink, ie, data come IN   (rf/subscribe ...)

    you PUBLISH to a :port/source, ie, data goes OUT    (rf/dispatch ...)

    you do BOTH with :port/source-sink (both)           should we even have this, or should we spell out both directions?

the question about :port/source-sink arises because building the layout (the call for the UI itself) doesn't actually
need to make a distinction (in fact the code is a bit cleaner if we don't) and we have the callee sort it out (since it
needs to implement the correct usage anyway). The flow-diagram, on the other hand, is easier if we DO make the
distinction, so we can quickly build all the Nodes and Handles used for the diagram...
"
  (:require [bh.ui-component.atom.bh.table :as bh-table]
            [bh.ui-component.atom.diagram.editable-digraph :as digraph]
            [bh.ui-component.atom.diagram.diagram.composite-dag-support :as dag-support]
            [bh.ui-component.atom.experimental.ui-element :as e]
            [bh.ui-component.atom.re-com.label :as rc-label]
            [bh.ui-component.atom.re-com.slider :as rc-slider]
            [bh.ui-component.atom.re-com.table :as rc-table]
            [bh.ui-component.atom.resium.globe :as r-globe]
            [bh.ui-component.atom.worldwind.globe :as ww-globe]
            [bh.ui-component.atom.leaflet.globe :as l-globe]
            [bh.ui-component.molecule.composite.util.digraph :as dig]
            [bh.ui-component.molecule.composite.util.signals :as sig]
            [bh.ui-component.molecule.composite.util.ui :as ui]
            [bh.ui-component.utils :as ui-utils]
            [bh.ui-component.atom.chart.bar-chart :as bar-chart]
            [bh.ui-component.atom.chart.area-chart :as area-chart]
            [bh.ui-component.atom.chart.colored-pie-chart :as colored-pie-chart]
            [bh.ui-component.atom.chart.line-chart :as line-chart]
            [bh.ui-component.atom.component-registry :as registry]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [loom.graph :as lg]
            [re-com.core :as rc]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [taoensso.timbre :as log]
            [woolybear.ad.containers :as containers]
            [woolybear.ad.layout :as layout]))


(log/info "bh.ui-component.molecule.composite")


(def component-needs {:ui/component  {:name :id}
                      :source/remote {:name :id}
                      :source/local  {:name :id :default {:choices #{"" 0 [] {} #{}}}}
                      :source/fn     {:name  :id
                                      :ports {:name :id
                                              :type #{:port/source :port/sink :port/source-sink}}}})


(def source-code '[composite
                   :data component/ui-definition
                   :component-id :container.component
                   :container-id :container])


(defn config [full-config]
  {:blackboard {:defs {:source full-config
                       :dag    {:open-details ""}}}
   :container  ""})


(defn definition-panel
  "show the text definition of the composed UI
  "
  [& {:keys [configuration]}]

  (let [components (:components configuration)
        links      (:links configuration)
        layout     (:grid-layout configuration)]

    ;(log/info "definition-panel" components)
    ;(log/info "definition-panel" links)
    ;(log/info "definition-panel" layout)

    (fn [& {:keys [configuration]}]
      [rc/v-box :src (rc/at)
       :width "70%"
       ;:height "100%"
       :gap "10px"
       :children [[:h3 "Components"]
                  [containers/v-scroll-pane {:height "10em"}
                   [layout/text-block (str components)]]

                  [:h3 "Links"]
                  [containers/v-scroll-pane {:height "10em"}
                   [layout/text-block (str links)]]

                  [:h3 "Layout"]
                  [containers/v-scroll-pane {:height "10em"}
                   [layout/text-block (str layout)]]]])))


(defn dag-panel
  "show the DAG, built form the configuration passed into the component, in a panel
  (beside the actual UI)
  "
  [& {:keys [configuration component-id container-id ui]}]
  (let [flow           (r/atom (ui/make-flow configuration))
        node-types     {":ui/component"  (partial ui/custom-node :ui/component)
                        ":source/remote" (partial ui/custom-node :source/remote)
                        ":source/local"  (partial ui/custom-node :source/local)
                        ":source/fn"     (partial ui/custom-node :source/fn)}
        minimap-styles {:nodeStrokeColor  (partial dag-support/custom-minimap-node-color
                                            dag-support/default-color-pallet digraph/color-white)
                        :node-color       (partial dag-support/custom-minimap-node-color
                                            dag-support/default-color-pallet digraph/color-black)
                        :nodeBorderRadius 5}]

    [digraph/component
     :component-id component-id
     :data flow
     :node-types node-types
     :tool-types dag-support/default-tool-types
     :minimap-styles minimap-styles]))





; RICH COMMENTS
;; region

;; basics of Loom (https://github.com/aysylu/loom)
(comment
  (do
    (def g (lg/graph [1 2] [2 3] {3 [4] 5 [6 7]} 7 8 9))
    (def dg (lg/digraph g))
    (def wg (lg/weighted-graph {:a {:b 10 :c 20} :c {:d 30} :e {:b 5 :d 5}}))
    (def wdg (lg/weighted-digraph [:a :b 10] [:a :c 20] [:c :d 30] [:d :b 10]))
    (def fg (lg/fly-graph :successors range :weight (constantly 77))))


  (lg/nodes g)
  (lg/edges g)
  (lg/has-node? g 5)
  (lg/weighted-graph g)

  (lg/nodes fg)

  ())


;; how do we use Loom for our composite?
(comment
  (def composite-def {})

  ; a Loom digraph only needs EDGES (:links)
  (def edges (->> composite-def
               :links
               (mapcat (fn [[entity links]]
                         (map (fn [[target port]]
                                [entity target])
                           links)))
               (into [])))


  ; with THIS set of edges, sources and sinks all look like successors
  (def g (apply lg/digraph edges))

  ())




;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;
;
; THIS IS THE ONE!!!
;
;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;
;; piece together the data needed to build all the UI components and supporting functions
(comment
  (do
    (def config @bh.ui-component.molecule.composite.coverage-plan/ui-definition)
    (def container-id "dummy")
    (def links (:links config))
    (def layout (:layout config))
    (def components (:components config))
    (def graph (apply lg/digraph (ui/compute-edges config)))
    (def nodes (lg/nodes graph))
    (def edges (lg/edges graph))
    (def registry @(rf/subscribe [:meta-data-registry]))

    (def configuration (assoc @bh.ui-component.molecule.composite.coverage-plan/ui-definition
                         :graph graph
                         :denorm (dig/denorm-components graph (:links config) (lg/nodes graph))
                         :nodes (lg/nodes graph)
                         :edges (lg/edges graph))))

  ;; 1. build the functions... (how? where?)
  ;; region
  ;;
  ;; actually, since the functions "subscribe" to some inputs and then produce something
  ;; that "others" subscribe to, they need to be "cascaded subscriptions" themselves,
  ;; so we will actually build the subscriptions alongside step 2, using the data we assemble here
  ;; (input signals, and the "subscription name(s)")
  ;;
  ;; NOTE: these functions need to produce ONE subscription for each :port/source
  ;;
  ;; for example,
  ;;
  ;;         {:fn/compute {:name some-computation
  ;;                       :ports {:input :port/sink
  ;;                               :computed-output :port/source}}}
  ;;
  ;; builds the equivalent of:
  ;;
  ;;         (re/frame/reg-sub-db
  ;;           :container/blackboard.computed-output
  ;;           :<- [:container/blackboard.input]
  ;;           (fn [input [_ _]]
  ;;              (some-computation* input))
  ;;
  ;;;
  ;; something like,
  ;;
  ;;         {:fn/compute {:name some-computation
  ;;                       :ports {:input-1 :port/sink
  ;;                               :input-2 :port/sink
  ;;                               :computed-output :port/source}}}
  ;;
  ;; would build the equivalent of:
  ;;
  ;;         (re/frame/reg-sub-db
  ;;           :container/blackboard.computed-output
  ;;           :<- [:container/blackboard.input-1]
  ;;           :<- [:container/blackboard.input-2]
  ;;           (fn [input-1 input-2 [_ _]]
  ;;              (some-computation* input-1 input-2))
  ;;
  ;; endregion


  ;; we could mix-in the "local name" for each link by mapping over the
  ;; successors and predecessors

  ;; actually, now it looks like :links already has all the names we need, we just need to
  ;; make the distinction between :inputs and :outputs for the flow-diagram (the UI Layout doesn't
  ;; need this data) But, I think we can use this function to split the flow-diagram data from the
  ;; UI-layout data (and putting BOTH into the expanded data configuration structure)


  ;; let's just use the terms :inputs and :outputs and drop the preds/succs

  ;; outputs
  ;; region
  (def source :fn/coverage)

  (->> links
    source
    (map (fn [[source-port target-meta]]
           (apply merge
             (map (fn [[target target-port]]
                    {target [source-port target-port]})
               target-meta))))
    (apply merge))


  {:fn/range {:outputs (dig/get-outputs links :fn/range)}}
  (dig/get-outputs links :fn/coverage)

  ; map over all the components (:ui/globe & :ui/current-time should have not outputs!)
  (->> nodes
    (map (fn [node]
           {node (dig/get-outputs links node)})))


  ;; endregion


  ;; inputs
  ;; region
  (def node :ui/globe)


  ; find components with :ui/globe as a target (ie. predecessors of :ui/globe
  (def preds (lg/predecessors* graph source))

  ; then map over then and pull out their source-port and the target meta-data for
  ; :ui/globe

  (->> node
    (lg/predecessors* graph)
    (map (fn [p]
           (apply merge
             (map (fn [[source-port targets]]
                    (let [target-port (get targets node)]
                      {p [source-port target-port]}))
               (get links p)))))
    (into {}))

  (:topic/coverage-data links)

  (dig/get-inputs links graph :ui/globe)
  (dig/get-inputs links graph :fn/range)


  (->> nodes
    (map (fn [node]
           {node (dig/get-inputs links graph node)})))

  ;; endregion


  ;; now put is all together
  ;; region

  (->> nodes
    (map (fn [node]
           {node
            {:inputs  (dig/get-inputs links graph node)
             :outputs (dig/get-outputs links node)}}))
    (into {}))

  (dig/denorm-components graph links nodes)

  ;; endregion

  ; GOT IT!
  ;
  ; we can now work from any node to its inputs and outputs,
  ; which means we can build the signal vectors for the ui elements
  ;
  ; AND a react-flow diagram of the event-mode for the UI!
  ;


  ; QUESTION: should we mix-in the notion of :local and :remote right here, so we can
  ; build the correct subscription/event signals?
  ;
  ; OR we can leave that logic to the function that actually builds the signal vectors (see
  ;    Step 2)
  ;        THIS requires looking at the component's meta-data
  ;
  ; OR we could leave it to the component itself to build the correct vector(s)
  ;
  ; what about the flow-diagram?
  ;


  ; 2. build the subscription and event signal vectors (just call them)
  ;; region
  (defn dummy [& {:keys [data range]}]
    {:data data :range range})

  (def target :topic/coverage-data)
  (def thing {:data [:topic/coverage-data], :range [:topic/time-range]})

  (flatten (seq thing))

  (apply conj [:dummy] (flatten (seq thing)))


  (->> components
    (filter (fn [[node meta-data]]
              (= :ui/component (:type meta-data))))
    (map (fn [[node meta-data]]
           (sig/component->ui {:node          node
                               :type          (:type meta-data)
                               :configuration configuration
                               :registry      @(rf/subscribe [:meta-data-registry])
                               :container-id  :dummy}))))

  ;; endregion


  ; the correct order of operations is:
  ;
  ; 1. remote subscriptions (including the remote call)
  ;
  ; [SIDE EFFECT]
  (sig/process-components configuration :source/remote @(rf/subscribe [:meta-data-registry]) :coverage-plan)

  ; 1a. build the subscription for the "container" which provide the basis for the
  ;     subscriptions for the "locals"
  ;
  ; [SIDE EFFECT]
  (ui-utils/create-container-sub container-id)


  ; 2. add blackboard data to the app-db and build local subscriptions/events against the blackboard
  ;
  ; [SIDE EFFECT]
  (sig/process-components configuration :source/local @(rf/subscribe [:meta-data-registry]) :coverage-plan)

  ; 3. local functions (to build subscriptions against the blackboard or remotes)
  ;
  ; [SIDE EFFECT]
  (sig/process-components configuration :source/fn @(rf/subscribe [:meta-data-registry]) :coverage-plan)

  ; 4. build UI components (with subscriptions against the blackboard or remotes)
  ;
  ;      actually, this can happen at any time, since evaluation is deferred to Reagent upon re-render
  ;
  ; this just builds the vectors and maps them to the component-id in the configuration in pre for Step 5
  ;
  (def component-lookup (into {}
                          (sig/process-components
                            configuration :ui/component
                            @(rf/subscribe [:meta-data-registry]) :coverage-plan)))

  ; 5. run layout over the UI components using component-lookup
  ;



  ())


; get the different handle names, so we can put multiple handles on a single node
; and then also connect the different edges to the correct one
(comment
  (def configuration @bh.ui-component.molecule.composite.coverage-plan/ui-definition)
  (def node-id :ui/globe)
  (def target-id :topic/selected-coverages)

  (or (get-in configuration [:links node-id target-id])
    (get-in configuration [:links target-id node-id]))

  ())


; new logic for building the flow-nodes, so we can have custom node rendering
(comment
  (do
    (def node-id :fn/range)
    (def graph (apply lg/digraph (ui/compute-edges @bh.ui-component.molecule.composite.coverage-plan/ui-definition)))
    (def configuration
      (assoc @bh.ui-component.molecule.composite.coverage-plan/ui-definition
        :graph graph
        :denorm (dig/denorm-components graph (:links configuration) (lg/nodes graph))
        :nodes (lg/nodes graph)
        :edges (lg/edges graph))))

  (:denorm configuration)

  (get-in configuration [:denorm :fn/coverage :inputs])

  (def components (:components configuration))


  (map (fn [[node meta-data]]
         ^{:key node} [:p (str node)])
    (filter (fn [[node {:keys [type]}]]
              (= :ui/component type))
      components))

  (keys components)


  ())


(comment
  (do
    (def data @bh.ui-component.molecule.composite.coverage-plan/ui-definition)
    (def graph (apply lg/digraph (ui/compute-edges @bh.ui-component.molecule.composite.coverage-plan/ui-definition)))
    (def nodes (lg/nodes graph))
    (def links (:links data))
    (def components (:components data))
    (def configuration (assoc @bh.ui-component.molecule.composite.coverage-plan/ui-definition
                         :components (dig/expand-components data @(rf/subscribe [:meta-data-registry]))
                         :graph graph
                         :nodes (lg/nodes graph)
                         :edges (lg/edges graph)))

    (def node-meta (->> links :ui/satellites)))


  (->> data
    :components
    (map (fn [[id meta-data]]
           {id (assoc meta-data
                 :ports
                 (condp = (:type meta-data)
                   :ui/component (->> components id :name @(rf/subscribe [:meta-data-registry]) :ports)
                   :source/remote {:port/pub-sub :data}
                   :source/local {:port/pub-sub :data}
                   :source/fn (:ports meta-data)))}))
    (assoc data :components))

  (dig/expand-components data @(rf/subscribe [:meta-data-registry]))

  (map #(assoc % :ports "x") (:components data))


  (def target-meta (map (fn [[target _]] (target @(rf/subscribe [:meta-data-registry]))) node-meta))

  (dig/denorm-components graph links nodes)


  ())


; token substitution
(comment
  (do
    (def config @bh.ui-component.molecule.composite.coverage-plan/ui-definition)
    (def container-id "dummy")
    (def links (:links config))
    (def layout (:layout config))
    (def components (:components config))
    (def graph (apply lg/digraph (ui/compute-edges config)))
    (def nodes (lg/nodes graph))
    (def edges (lg/edges graph))
    (def registry @(rf/subscribe [:meta-data-registry]))

    (def configuration (assoc @bh.ui-component.molecule.composite.coverage-plan/ui-definition
                         :graph graph
                         :denorm (dig/denorm-components graph (:links config) (lg/nodes graph))
                         :nodes (lg/nodes graph)
                         :edges (lg/edges graph)
                         :ui-lookup (into {}
                                      (sig/process-components
                                        configuration :ui/component
                                        @(rf/subscribe [:meta-data-registry]) :coverage-plan))))
    (def component-lookup (into {}
                            (sig/process-components
                              configuration :ui/component
                              @(rf/subscribe [:meta-data-registry]) :coverage-plan)))
    (def node :h-box))

  (:ui-lookup configuration)

  ;(parse-token component-lookup node)

  ;(into (parse-token lookup node) [:children [:a :b :c]])


  ())


; subscription scratchpad
(comment
  (ui-utils/subscribe-local :coverage-plan-demo.component
    [:blackboard :topic/current-time])

  (ui-utils/dispatch-local :coverage-plan-demo.component
    [:blackboard :topic/current-time] (js/Date.))


  (rf/reg-sub
    :coverage-plan-demo.component.blackboard.topic.time-range
    :<- [:coverage-plan-demo.component.blackboard.topic.current-time]
    (fn [t _]
      [0 t]))


  (rf/reg-sub
    :coverage-plan-demo.component.blackboard.topic.layers
    :<- [:coverage-plan-demo.component.blackboard.topic.selected-targets]
    :<- [:coverage-plan-demo.component.blackboard.topic.selected-satellites]
    :<- [:bh.subs/source :topic/coverage-data]
    (fn [t s c _]
      [{:layer-1 {} :layer-2 {}}]))


  (ui-utils/dispatch-local :coverage-plan-demo.component
    [:blackboard :topic/current-time] 75)


  (rf/subscribe [:coverage-plan-demo.component.blackboard.topic.current-time])
  (rf/subscribe [:coverage-plan-demo.component.blackboard.topic.selected-targets])
  (rf/subscribe [:coverage-plan-demo.component.blackboard.topic.selected-satellites])
  (rf/subscribe [:bh.subs/source :source/targets])
  (rf/subscribe [:bh.subs/source :source/satellites])
  (rf/subscribe [:bh.subs/source :source/coverages])

  (rf/subscribe [:coverage-plan-demo.component.blackboard.topic.layers])
  (rf/subscribe [:coverage-plan-demo.component.blackboard.topic.time-range])

  ())



; have to actually CALL the fn/subcription we built!
(comment
  (do
    (def config @bh.ui-component.molecule.composite.coverage-plan/ui-definition)
    (def container-id :coverage-plan-demo.component)
    (def links (:links config))
    (def layout (:layout config))
    (def components (:components config))
    (def graph (apply lg/digraph (dig/compute-edges config)))
    (def nodes (lg/nodes graph))
    (def edges (lg/edges graph))
    (def registry @(rf/subscribe [:meta-data-registry]))

    (def configuration (assoc @bh.ui-component.molecule.composite.coverage-plan/ui-definition
                         :graph graph
                         :denorm (dig/denorm-components graph (:links config) (lg/nodes graph))
                         :nodes (lg/nodes graph)
                         :edges (lg/edges graph)
                         :ui-lookup (into {}
                                      (sig/process-components
                                        configuration :ui/component
                                        @(rf/subscribe [:meta-data-registry]) :coverage-plan))))
    (def component-lookup (into {}
                            (sig/process-components
                              configuration :ui/component
                              @(rf/subscribe [:meta-data-registry]) :coverage-plan)))
    (def node :fn/range)

    (def actual-fn (->> configuration :components node :name))
    (def denorm (->> configuration :denorm node)))

  ())

;; endregion


(comment
  (def components [[[:div "1"] [empty] [:div "2"]]
                   [[empty] [:div "3"] [:div "4"]]])
  (layout/layout components)

  ())

; make sure the event handler preserves the ordering of the components in the DSL
(comment
  (def id "dummy")
  (def c {:containers {id (config id)}})

  ((partial apply conj) [] [[:a :b] [:c :d]])

  ; using DSL (in progress, see component-layout
  (def components [[[:div "1"]]
                   [[:div "2"]]])
  (def components [[[:div "1"] [empty] [:div "2"]]
                   [[:div "3"] [:div "4"]]])

  (update-in c [:containers id :components] (partial apply conj) components)

  ())


(comment



  ())