(ns bh.ui-component.molecule.composite.coverage-plan
  "provide a composed UI for a \"Coverage Plan\" which shows targets and satellite coverage areas
  on a 3D globe"
  (:require [bh.ui-component.atom.bh.color-picker :as picker]
            [bh.ui-component.molecule.composite.coverage-plan.support :as s]
            [bh.ui-component.utils :as ui-utils]
            [bh.ui-component.utils.color :as c]
            [bh.ui-component.utils.helpers :as h]
            [cljs-time.coerce :as coerce]
            [cljs-time.core :as t]
            [re-com.core :as rc]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [taoensso.timbre :as log]
            ["dagre" :as dagre]
            ["graphlib" :as graphlib]
            ["react-flow-renderer" :refer (ReactFlowProvider Controls Handle Background) :default ReactFlow]))


(log/info "bh.ui-component.molecule.composite.coverage-plan")


;; region ; data for developing the UI


(def dummy-targets #{"alpha-hd" "bravo-img" "fire-hd" "fire-ir" "severe-hd"})


(def dummy-satellites #{"abi-meso-2" "abi-meso-10" "abi-meso-4" "abi-meso-11" "viirs-5" "avhhr-6"})

;; endregion


;; region ; local function to support :source/local topics

(defn fn-coverage
  "registers the subscription for the entity defined by 'layers'. processing from
  inputs to the output is performed by 'some-computation'

  this function assumes that the caller provides fully-qualified signal vectors, so the CALLER
  is responsible for building the keyword (using path->keyword)

  - targets : (vector of keywords) the subscription signal for the target data
  - satellites : (vector of keywords) the subscription signal for the 'selected' satellite data
  - coverages : (vector of keywords) the subscription signal for the coverage data

  builds and registers the subscription provided by 'layers'

  "
  [{:keys [targets satellites
           selected-targets selected-satellites
           coverages
           current-time shapes]}]

  ;(log/info "fn-coverage" shapes
  ;  "//" targets
  ;  "//" satellites
  ;  "//" selected-targets
  ;  "//" selected-satellites
  ;  "//" coverages)

  (re-frame/reg-sub
    (first shapes)
    :<- targets
    :<- satellites
    :<- selected-targets
    :<- selected-satellites
    :<- coverages
    :<- current-time
    (fn [[t s s-t s-s c ct] _]
      ;(log/info "fn-coverage (sub)" ct
      ;  "// (targets)" t
      ;  "// (satellites)" s
      ;  "// (selected-targets)" s-t
      ;  "// (selected-satellites)" s-s
      ;  "// (cooked)" (s/cook-coverages s s-s c ct)
      ;  "// (:topic/target-filter)" (filter #(contains?
      ;                                         s-s (get-in % [:coverage :sensor]))
      ;                                (s/cook-coverages s s-s c ct))

      (let [filtered-coverages (filter #(contains?
                                          s-s
                                          (get-in % [:coverage :sensor]))
                                 (s/cook-coverages s s-s c ct))
            cvg                (if (seq filtered-coverages)
                                 (map s/make-coverage-shape filtered-coverages)
                                 [])
            trg                (if (seq t)
                                 (map s/make-target-shape (s/cook-targets t s-t ct))
                                 [])
            ret                (concat cvg trg)]

        ;(log/info "fn-coverage (ret)" ret
        ;  "//" cvg
        ;  "//" trg)

        ret))))


(defn fn-range
  "registers the subscription for the entity defined by 'selected'. processing from
  inputs to output is performed by 'some-computation'

  - data : (vector of keywords) the subscription signal for the input data
  - container-id : (string) name of the container holding the blackboard

  builds and registers the subscription :<container>/blackboard.<selected>
  "
  [{:keys [data range]}]

  ;(log/info "fn-range" range "//" data)

  (re-frame/reg-sub
    (first range)
    :<- data
    (fn [d _]
      (let [times (->> d :data (map :time) set)]
        [(apply min times) (apply max times)]))))


(defn fn-current-time [{:keys [value current-time]}]
  ;(log/info "fn-current-time" value "//" current-time)

  (re-frame/reg-sub
    (first current-time)
    :<- value
    (fn [v _]
      (coerce/to-date (t/plus (t/now) (t/hours v))))))


(defn fn-color-targets [{:keys [data colored]}]
  (let [next-target-color (atom -1)
        [component topic] (-> colored
                            first
                            name
                            (clojure.string/split #".blackboard."))
        path              [(keyword (str component ".blackboard"))]]

    ;(log/info "fn-color-targets" data "//" colored "//" path)

    (re-frame/reg-sub
      (first colored)
      :<- data
      :<- path
      (fn [[d p] _]
        ;(log/info "fn-color-targets (data)" d "//" (:data d) "//" (keyword topic) "//" ((keyword topic) p))
        (let [cnt              (count s/sensor-color-pallet)
              last-target-data ((keyword topic) p)
              assigned         (map (juxt :name :color) last-target-data)
              assigned-set     (->> assigned (map first) set)

              ;_            (log/info "fn-color-targets (atom)" @last-target-data "//" assigned "//" assigned-set)
              ret              (doall
                                 (map (fn [t]
                                        (if (contains? assigned-set (:name t))
                                          (assoc t :color (->> last-target-data
                                                            (filter #(= (:name t) (:name %)))
                                                            first
                                                            :color))
                                          (assoc t :color (nth s/sensor-color-pallet
                                                            (mod (swap! next-target-color inc) cnt)))))
                                   (:data d)))]

          ;(log/info "fn-color-targets (ret)" ret)

          ; need to store this in the app-db because this fn is STATEFUL, we don't
          ; want to change a target if it has already been assigned a color
          (h/handle-change-path path [topic] ret)

          ret)))))


(defn fn-color-satellites [{:keys [data colored]}]
  ;(log/info "fn-color-satellites" data "//" colored)
  (let [next-sat-color (atom -1)
        [component topic] (-> colored
                            first
                            name
                            (clojure.string/split #".blackboard."))
        path           [(keyword (str component ".blackboard"))]]
    (re-frame/reg-sub
      (first colored)
      :<- data
      :<- path
      (fn [[d p] _]
        ;(log/info "fn-color-satellites (data)" d "//" (:data d))
        (let [cnt           (count s/sensor-color-pallet)
              last-sat-data ((keyword topic) p)
              assigned      (map (juxt :sensor_id :color) last-sat-data)
              assigned-set  (->> assigned (map first) set)
              ;_            (log/info "fn-color-satellites (atom)" @last-sat-data "//" assigned "//" assigned-set)
              ret           (doall
                              (map (fn [t]
                                     (if (contains? assigned-set (:sensor_id t))
                                       (assoc t :color (->> last-sat-data
                                                         (filter #(= (:sensor_id t) (:sensor_id %)))
                                                         first
                                                         :color))
                                       (assoc t :color (nth s/sensor-color-pallet
                                                         (mod (swap! next-sat-color inc) cnt)))))
                                (:data d)))]

          ;(log/info "fn-color-satellites (ret)" ret )

          ; need to store this in the app-db because this fn is STATEFUL, we don't
          ; want to change a satellite/sensor if it has already been assigned a color
          (h/handle-change-path path [topic] ret)

          ret)))))


(defn fn-filtered-targets [{:keys [targets filter-value filtered-targets] :as params}]
  (re-frame/reg-sub
    (first filtered-targets)
    :<- targets
    :<- filter-value
    (fn [[t f] _]
      (->> t
        (filter #(re-find (re-pattern (str "(?i)" f)) (:name %)))))))


; register meta-data for all these functions
;
(re-frame/dispatch-sync [:register-meta
                         {:coverage-plan/fn-coverage {:function fn-coverage
                                                      :ports {:targets          :port/sink :satellites :port/sink
                                                              :selected-targets :port/sink :selected-satellites :port/sink
                                                              :coverages        :port/sink :current-time :port/sink
                                                              :shapes           :port/source}}
                          :coverage-plan/fn-range {:function fn-range
                                                   :ports {:data :port/sink
                                                           :range :port/source}}
                          :coverage-plan/fn-current-time {:function fn-current-time
                                                          :ports {:value :port/sink
                                                                  :current-time :port/source}}
                          :coverage-plan/fn-color-targets {:function fn-color-targets
                                                           :ports {:data :port/sink
                                                                   :colored :port/source}}
                          :coverage-plan/fn-color-satellites {:function fn-color-satellites
                                                              :ports {:data :port/sink
                                                                      :colored :port/source}}
                          :coverage-plan/fn-filtered-targets {:function fn-filtered-targets
                                                              :ports {:targets          :port/sink
                                                                      :filter-value     :port/sink
                                                                      :filtered-targets :port/source}}}])

;; endregion


;; region ; custom tables for display


(defn- update-color [data id id-key color-type new-color]
  ;(log/info "update-color" id color-type new-color)
  (let [path      (-> data
                    first
                    name
                    (clojure.string/split #".blackboard.")
                    (#(map keyword %))
                    ((fn [[c p]] [c :blackboard p])))
        orig-data (h/resolve-value data)
        target    (first (filter #(= (id-key %) id) @orig-data))
        kept      (remove #(= (id-key %) id) @orig-data)
        new-data  (conj kept (assoc target :color (case color-type
                                                    :hex (c/match-colors-hex new-color)
                                                    :rgba (c/match-colors-rgba new-color)
                                                    (c/match-colors-hex new-color))))]

    ;(log/info "update-color (path)" id "//" path "//" new-data)
    (h/handle-change-path path [] new-data)))


(defn- toggle-selection [resolved-selection selection-path id]
  (let [s-ids (or resolved-selection #{})]
    (if (contains? resolved-selection id)
      ; remove
      (h/handle-change-path selection-path [] (disj s-ids id))

      ; add
      (h/handle-change-path selection-path [] (conj s-ids id)))))


(defn- display-checkbox [id name under-consideration toggle-fn]
  ^{:key (str "check-" id)}
  [:td.is-narrow
   {:style    {:text-align :center}
    :on-click (rc/handler-fn
                (toggle-fn))}

   (if (contains? under-consideration id)
     [:span.icon.has-text-success.is-small [:i.fas.fa-check]]
     [:span.icon.has-text-success.is-small [:i.far.fa-square]])])


(defn- display-symbol [data update-colors name [_ _ _ _ color]]
  (let [showing? (r/atom false)
        d        (h/resolve-value data)]

    ;(log/info "display-symbol" data "//" name "//" @showing?)

    (fn [data update-colors name [_ _ _ _ color]]

      ;(log/info "display-symbol (inner)" name
      ;"//" color "//" @d
      ;"//" @showing?

      ^{:key (str "symb-" name)}
      [:td {:style {:color      :white
                    :text-align :center}}
       [rc/popover-anchor-wrapper :src (rc/at)
        :showing? @showing?
        :position :below-right
        :anchor [:span.icon.has-text-success.is-small
                 [:i.fas.fa-circle
                  {:style    {:color color}
                   :on-click #(do
                                (swap! showing? not))}]]
        ;(log/info "display-symbol (click)" name "//" @showing?))}]]
        :popover [rc/popover-content-wrapper :src (rc/at)
                  :close-button? false
                  :no-clip? false
                  :body [picker/hex-color-picker
                         :color color
                         :on-change (fn [x]
                                      (update-color update-colors name :name :hex (js->clj x)))]]]])))


(defn- display-edit-control [name]
  (let [is-editing (r/atom false)]
    (fn []
      ^{:key (str "edit-" name)}
      [:td {:on-click #(if @is-editing
                         (do
                           (log/info "SAVE" name)
                           (reset! is-editing false))
                         (do
                           (log/info "EDIT" name)
                           (reset! is-editing true)))}
       (if @is-editing
         [:span.icon.has-text-success.is-small [:i.far.fa-save]]
         [:span.icon.has-text-info.is-small [:i.far.fa-edit]])])))


(defn- display-delete-control [name]
  ^{:key (str "delete-" name)}
  [:td {:on-click #(do)}
   [:span.icon.has-text-danger.is-small [:i.far.fa-trash-alt]]])


(defn- display-color [data name [_ js-color rgba-color _ icon-color]]

  (let [showing? (r/atom false)
        d        (h/resolve-value data)]

    ;(log/info "display-color" name "//" @showing?)

    (fn [data name [_ js-color [r g b a] _ icon-color]]

      ;(log/info "display-color (inner)" name
      ;"//" js-color "//" @d
      ;"//" @showing?

      ^{:key (str "color-" name)}
      [:td
       {:style {:background-color :transparent
                :border-width     "1px"
                :text-align       :center
                :width            100}}
       [rc/popover-anchor-wrapper :src (rc/at)
        :showing? @showing?
        :position :below-right
        :anchor [:div {:style    {:padding          "5px 10px 5px 10px"
                                  :background-color (or js-color :green)}
                       :on-click #(swap! showing? not)}
                 name]
        :popover [rc/popover-content-wrapper :src (rc/at)
                  :close-button? false
                  :no-clip? false
                  :body [picker/rgba-color-picker
                         :color {:r r :g g :b b :a a}
                         :on-change (fn [x]
                                      (update-color data name :sensor_id :rgba
                                        (js->clj x :keywordize-keys true)))]]]])))


(defn- display-text [cell]
  ^{:key (str "target-" cell)} [:td cell])


(def column-types {:column/boolean      display-checkbox
                   :column/colored-cell display-color
                   :column/colored-icon display-symbol
                   :column/delete       display-delete-control
                   :column/edit         display-edit-control
                   :column/text         display-text
                   :column/status       #()})


(def target-row-def {:columns    [{:column/key :include :column/label "Include?" :column/type :cell/boolean}
                                  {:column/key :symbol :column/label "Symbol" :column/type :cell/colored-icon :column/icon :i.fas.fa-circle}
                                  {:column/key :aoi :column/label "AoI" :column/type :cell/text}
                                  {:column/key :edit :column/label "" :column/type :cell/edit-toggle}
                                  {:column/key :delete :column/label "" :column/type :cell/delete-toggle}]
                     :unique-key :name})


(def satellite-row-def {:columns    [{:column/key :include :column/label "Include?" :column/type :cell/boolean}
                                     {:column/key :sensor_id :column/label "Sensor/Color" :column/type :cell/colored-cell}
                                     {:column/key :platform_id :column/label "Platform" :column/type :cell/text}]
                        :unique-key :sensor_id})


(defn- column-header-cell [column]
  ^{:key (str (:column/label column) "-" (rand-int 1000))} [:th (:column/label column)])


(defn- target-table [& {:keys [data selection colors component-id container-id] :as params}]
  ;(log/info "target-table" params)
  (let [d (h/resolve-value data)
        s (h/resolve-value selection)]

    (fn []
      ;(log/info "target-table (d)" @d "//" @s)
      [:div.table-container {:style {:width       "100%"
                                     :height      "100%"
                                     :overflow-y  :auto
                                     :white-space :nowrap
                                     :border      "1px outset gray"}}
       [:table.table
        [:thead {:style {:position :sticky :top 0 :background :lightgray}}
         [:tr
          (doall (->> target-row-def :columns (map column-header-cell)))]]

        [:tbody
         (doall
           (for [{:keys [name cells color] :as target} @d]
             (doall
               ;(log/info "target-table (for)" @d
               ; "//" target "//" name "//" color)

               ^{:key name}
               [:tr
                [display-checkbox name name @s #(toggle-selection @s selection name)]

                [display-symbol data colors name color]

                [display-text name]

                [display-edit-control name]

                [display-delete-control name]])))]]])))


(defn- satellite-table [& {:keys [data selection component-id container-id]}]
  ;(log/info "satellites-table" data "//" selection)

  (let [d (h/resolve-value data)
        s (h/resolve-value selection)]

    ;(log/info "satellites-table (s)" @d "//" @s)

    (fn []
      [:div.table-container {:style {:width       "100%"
                                     :height      "100%"
                                     :overflow-y  :auto
                                     :white-space :nowrap
                                     :border      "1px outset gray"}}
       [:table.table
        [:thead {:style {:position :sticky :top 0 :background :lightgray}}
         [:tr
          (doall (->> satellite-row-def :columns (map column-header-cell)))]]

        [:tbody
         (doall
           (for [{:keys [platform_id sensor_id color] :as platform} @d]
             (doall
               ^{:key sensor_id}
               [:tr
                [display-checkbox sensor_id (str platform_id "-" sensor_id)
                 @s #(toggle-selection @s selection sensor_id)]

                [display-color data sensor_id color]

                ^{:key (str "satellite-" platform_id "-" sensor_id)}
                [display-text platform_id]])))]]])))


(defn- target-filter-input [& {:keys [value component-id container-id] :as params}]
  (let [v (h/resolve-value value)]
    (fn []
      [rc/h-box :src (rc/at)
       :align :center
       :children [[rc/input-text :src (rc/at)
                   :model @v
                   :placeholder "enter text to filter targets"
                   :change-on-blur? false
                   :on-change #(h/handle-change-path value [] %)]
                  [rc/md-circle-icon-button :src (rc/at)
                   :md-icon-name "zmdi-close-circle-o"
                   :tooltip "Click to clear"
                   :size :smaller
                   :on-click #(h/handle-change-path value [] "")]]])))


; register these as UI-components
(re-frame/dispatch-sync [:register-meta
                         {:coverage-plan/target-table {:component target-table
                                                       :ports {:data :port/sink
                                                               :selection :port/sink
                                                               :colors :port/sink}}
                          :coverage-plan/satellite-table {:component satellite-table
                                                          :ports {:data :port/sink
                                                                  :selection :port/sink}}
                          :coverage-plan/target-filter-input {:component target-filter-input
                                                              :ports {:value :port/sink}}}])


;; endregion


(def ui-definition {:title        "Coverage Plan"
                    :component-id :coverage-plan
                    :components   {; ui components
                                   ; TODO: add a :label element for use in the UI
                                   :ui/targets                {:type        :ui/component :name :coverage-plan/target-table
                                                               :label       "Targets"
                                                               :config-data {:columns    [{:column/key :include :column/label "Include?" :column/type :cell/boolean}
                                                                                          {:column/key :symbol :column/label "Symbol" :column/type :cell/colored-icon :column/icon :i.fas.fa-circle}
                                                                                          {:column/key :aoi :column/label "AoI" :column/type :cell/text}
                                                                                          {:column/key :edit :column/label "" :column/type :cell/edit-toggle}
                                                                                          {:column/key :delete :column/label "" :column/type :cell/delete-toggle}]
                                                                             :unique-key :name}}
                                   :ui/satellites             {:type :ui/component :name :coverage-plan/satellite-table :label "Platforms"}
                                   :ui/globe                  {:type :ui/component :name :ww/globe}
                                   :ui/time-slider            {:type :ui/component :name :rc/slider}
                                   :ui/current-time           {:type :ui/component :name :rc/label-md}
                                   :ui/target-filter          {:type :ui/component :name :coverage-plan/target-filter-input :label "Filter:"}

                                   ; remote data sources
                                   :topic/target-data         {:type :source/remote :name :source/targets}
                                   :topic/satellite-data      {:type :source/remote :name :source/satellites}
                                   :topic/coverage-data       {:type :source/remote :name :source/coverages}

                                   ; composite-local data sources
                                   :topic/selected-targets    {:type :source/local :name :selected-targets :default dummy-targets}
                                   :topic/colored-targets     {:type   :source/local :name :colored-targets
                                                               :fields [:target/include :target/symbol :target/name]}

                                   :topic/selected-satellites {:type :source/local :name :selected-satellites :default dummy-satellites}
                                   :topic/colored-satellites  {:type :source/local :name :colored-satellites}

                                   :topic/current-time        {:type :source/local :name :current-time :default 0}
                                   :topic/shapes              {:type :source/local :name :shapes}
                                   :topic/time-range          {:type :source/local :name :time-range}
                                   :topic/current-slider      {:type :source/local :name :current-slider :default 0}
                                   :topic/target-filter       {:type :source/local :name :target-filter :default ""}
                                   :topic/filtered-targets    {:type :source/local :name :filtered-targets}

                                   ; transformation functions
                                   :fn/coverage               {:type  :source/fn :name :coverage-plan/fn-coverage}
                                   :fn/range                  {:type  :source/fn :name :coverage-plan/fn-range}
                                   :fn/current-time           {:type  :source/fn :name :coverage-plan/fn-current-time}
                                   :fn/color-targets          {:type  :source/fn :name :coverage-plan/fn-color-targets}
                                   :fn/color-satellites       {:type  :source/fn :name :coverage-plan/fn-color-satellites}
                                   :fn/filtered-targets       {:type  :source/fn :name :coverage-plan/fn-filtered-targets}}

                    :links        {:ui/targets                {;:data      {:topic/target-data :data}
                                                               :selection {:topic/selected-targets :data}
                                                               :colors    {:topic/colored-targets :data}}
                                   :ui/satellites             {;:data      {:topic/satellite-data :data}
                                                               :selection {:topic/selected-satellites :data}}
                                   :ui/time-slider            {:value {:topic/current-slider :data}}
                                   :ui/target-filter          {:value {:topic/target-filter :data}}

                                   ; transformation functions publish to what?
                                   :fn/coverage               {:shapes {:topic/shapes :data}}
                                   :fn/range                  {:range {:topic/time-range :data}}
                                   :fn/current-time           {:current-time {:topic/current-time :data}}
                                   :fn/color-targets          {:colored {:topic/colored-targets :data}}
                                   :fn/color-satellites       {:colored {:topic/colored-satellites :data}}
                                   :fn/filtered-targets       {:filtered-targets {:topic/filtered-targets :data}}

                                   ; topics are inputs into what?
                                   :topic/target-data         {:data {:fn/color-targets :data}}
                                   :topic/colored-targets     {:data {:fn/filtered-targets :targets}}
                                   :topic/selected-targets    {:data {:ui/targets  :selection
                                                                      :fn/coverage :selected-targets}}


                                   :topic/satellite-data      {:data {:fn/color-satellites :data}}
                                   :topic/colored-satellites  {:data {:ui/satellites :data
                                                                      :fn/coverage   :satellites}}
                                   :topic/selected-satellites {:data {:ui/satellites :selection
                                                                      :fn/coverage   :selected-satellites}}

                                   :topic/coverage-data       {:data {:fn/coverage :coverages
                                                                      :fn/range    :data}}
                                   :topic/shapes              {:data {:ui/globe :shapes}}
                                   :topic/current-time        {:data {:ui/current-time :value
                                                                      :ui/globe        :current-time}}
                                   :topic/current-slider      {:data {:fn/current-time :value
                                                                      :ui/time-slider  :value
                                                                      :fn/coverage     :current-time}}
                                   :topic/time-range          {:data {:ui/time-slider :range}}
                                   :topic/target-filter       {:data {:ui/target-filter    :value
                                                                      :fn/filtered-targets :filter-value}}
                                   :topic/filtered-targets    {:data {:ui/targets  :data
                                                                      :fn/coverage :targets}}}

                    :grid-layout  [{:i :ui/target-filter :x 5 :y 0 :w 4 :h 2 :static true}
                                   {:i :ui/targets :x 0 :y 2 :w 9 :h 7 :static true}
                                   {:i :ui/satellites :x 0 :y 9 :w 9 :h 8 :static true}
                                   {:i :ui/time-slider :x 2 :y 17 :w 6 :h 2 :static true}
                                   {:i :ui/globe :x 9 :y 0 :w 11 :h 17 :static true}
                                   {:i :ui/current-time :x 9 :y 17 :w 8 :h 2 :static true}]})



;; region ; Rich comments

; work out making actual shapes for the coverage data we get from the server
(comment
  (do
    (def coverages (get-in @re-frame.db/app-db [:sources :source/coverages :data]))
    (def current-time 0)

    (def time-coverage (filter #(= (:time %) current-time) coverages)))


  (ui-utils/subscribe-local
    :ui-grid-ratom-demo.coverage-plan
    [:blackboard :topic.shapes])


  ())


; can we cache the results so we only add :color to "new" elements?
(comment
  (do
    (def colored [:coverage-plan-demo-ww.grid-widget.blackboard.topic.colored-targets]) ; actual
    (def colored [:coverage-plan-demo-ww.grid-widget :blackboard :topic.colored-targets]) ; what we'd prefer

    (def last-data (atom []))
    (def last-target-data (atom dummy-targets))
    (def assigned (map (juxt :name :color) @last-data))
    (def assigned-set (->> assigned (map first) set))
    (def candidate {:name  "alpha-hd",
                    :cells #{[7 7 "hidef-image" 0] [7 6 "hidef-image" 1]
                             [7 5 "hidef-image" 3] [7 6 "hidef-image" 2]}}))

  (-> colored
    first
    name
    (clojure.string/split #".blackboard.")
    (#(map keyword %))
    ((fn [[c p]] [c :blackboard p])))

  (let [[component topic] (-> colored
                            first
                            name
                            (clojure.string/split #".blackboard."))]
    {:c [(keyword (str component ".blackboard"))] :t (keyword topic)})


  (if (contains? assigned-set (:name candidate))
    candidate
    (assoc candidate :color [:dummy :dummy :dummy]))


  (def last-target-data (atom []))

  (->> @last-target-data
    (filter #(= (:name candidate) (:name %)))
    first
    :color)


  @(re-frame/subscribe [:bh.subs/source :source/targets])

  ; can we change the data in :source/targets and have the UI update?
  ;
  (re-frame/dispatch [:bh.events/data-update
                      {:id    :source/targets
                       :value {:title    "Targets",
                               :c-o-c    [{:step      :generated,
                                           :by        "dummy.data-source.targets",
                                           :version   "0.6.0",
                                           :at        "Mon May 23 14:21:10 EDT 2022",
                                           :signature "73f7a470-ddc9-44d9-84fa-cf1ce1acc8f9"}],
                               :metadata {:title "Targets", :type :tabular, :id :target, :fields {:target :string, :cells :string}},
                               :data     [{:name  "alpha-hd",
                                           :cells #{[7 7 "hidef-image" 0] [7 6 "hidef-image" 1] [7 5 "hidef-image" 3] [7 6 "hidef-image" 2]}}
                                          {:name "bravo-img", :cells #{[7 2 "image" 0] [7 1 "image" 1]}}
                                          {:name  "fire-hd",
                                           :cells #{[5 3 "hidef-image" 2]
                                                    [4 3 "hidef-image" 3]
                                                    [4 3 "hidef-image" 2]
                                                    [5 3 "hidef-image" 0]
                                                    [5 3 "hidef-image" 3]}}
                                          {:name "fire-ir", :cells #{[5 4 "v/ir" 2] [5 4 "v/ir" 1] [5 3 "v/ir" 1] [5 4 "v/ir" 0] [5 4 "v/ir" 3]}}
                                          {:name  "severe-hd",
                                           :cells #{[5 7 "hidef-image" 3]
                                                    [5 6 "hidef-image" 0]
                                                    [6 6 "hidef-image" 2]
                                                    [6 5 "hidef-image" 1]
                                                    [5 7 "hidef-image" 1]}}
                                          {:name  "new-target",
                                           :cells #{[0 0 "hidef-image" 0]}}]}}])



  (get-in @re-frame.db/app-db '(:containers))
  (get-in @re-frame.db/app-db '(:containers :coverage-plan-demo-ww.grid-widget))
  (get-in @re-frame.db/app-db '(:containers :coverage-plan-demo-ww.grid-widget
                                 :blackboard))
  (get-in @re-frame.db/app-db '(:containers :coverage-plan-demo-ww.grid-widget
                                 :blackboard :topic.colored-targets))

  ; can we change the data in :topic/colored-targets?
  ;
  (def new-colors [{:name  "alpha-hd", :cells #{[7 7 "hidef-image" 0] [7 6 "hidef-image" 1]
                                                [7 5 "hidef-image" 3] [7 6 "hidef-image" 2]},
                    :color [:darkred "rgba(139, 0, 0, .3)" [139 0 0 0.3] [0.55 0.0 0.0 0.1] "#8B0000"]}
                   {:name  "bravo-img", :cells #{[7 2 "image" 0] [7 1 "image" 1]},
                    :color [:blue "rgba(0, 0, 255, .3)" [0 0 255 0.3] [0 0 1 0.1] "#0000FF"]}
                   {:name  "fire-hd", :cells #{[5 3 "hidef-image" 2] [4 3 "hidef-image" 3]
                                               [4 3 "hidef-image" 2] [5 3 "hidef-image" 0] [5 3 "hidef-image" 3]},
                    :color [:orange "rgba(255, 165, 0, .3)" [255 165 0 0.3] [1 0.65 0 0.3] "#FFA500"]}
                   {:name  "fire-ir", :cells #{[5 4 "v/ir" 2] [5 4 "v/ir" 1] [5 3 "v/ir" 1]
                                               [5 4 "v/ir" 0] [5 4 "v/ir" 3]},
                    :color [:grey "rgba(128, 128, 128, .3)" [128 128 128 0.3] [0.5 0.5 0.5 0.3] "#808080"]}
                   {:name  "severe-hd", :cells #{[5 7 "hidef-image" 3] [5 6 "hidef-image" 0]
                                                 [6 6 "hidef-image" 2] [6 5 "hidef-image" 1] [5 7 "hidef-image" 1]},
                    :color [:cornflowerblue "rgba(100, 149, 237, .3)"
                            [100 149 237 0.3] [0.4 0.58 0.93 0.3] "#6495ED"]}])

  (h/handle-change-path [:coverage-plan-demo-ww.grid-widget :blackboard :topic.colored-targets] []
    new-colors)

  ())


; update the correct target's color with the new value
(comment
  (do
    (def data [:coverage-plan-demo-ww.grid-widget.blackboard.topic.colored-targets])
    (def id "alpha-hd")
    (def new-color "#000000"))


  (let [path      (-> data
                    first
                    name
                    (clojure.string/split #".blackboard.")
                    (#(map keyword %))
                    ((fn [[c p]] [c :blackboard p])))
        orig-data (h/resolve-value data)
        target    (first (filter #(= (:name %) id) @orig-data))
        kept      (remove #(= (:name %) id) @orig-data)
        new-data  (conj kept (assoc target :color (c/match-colors-hex new-color)))]
    [target kept new-data])

  (update-color data id :name :hex new-color)


  ())


;; components have "ports" which define their inputs and outputs:
;;
;;      you SUBSCRIBE with a :port/sink, ie, data come IN   (re-frame/subscribe ...)
;;
;;      you PUBLISH to a :port/source, ie, data goes OUT    (re-frame/dispatch ...)
;;
;;      you do BOTH with :port/source-sink (both)           should we even have this, or should we spell out both directions?
;;
;; the question about :port/source-sink arises because building the layout (the call for the UI itself) doesn't actually
;; need to make a distinction (in fact the code is a bit cleaner if we don't) and we have the callee sort it out (since it
;; needs to implement the correct usage anyway). The flow-diagram, on the other hand, is easier if we DO make the
;; distinction, so we can quickly build all the Nodes and Handles used for the diagram...



;; endregion

