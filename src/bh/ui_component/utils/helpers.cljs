(ns bh.ui-component.utils.helpers
  (:require [bh.ui-component.atom.bh.navbar :as navbar]
            [cljs-uuid-utils.core :as uuid]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [re-com.core :as rc]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [taoensso.timbre :as log]
            [woolybear.packs.tab-panel :as tab-panel]))


(defn config-tab-panel [component-id]
  {:tab-panel {:value     (keyword component-id "config")
               :data-path [:containers (keyword component-id) :tab-panel]}})


(defn component-id []
  (-> (uuid/make-random-uuid)
    uuid/uuid-string))


(defn chart-config [[config data panel tab] data-panel config-panel]
  ;(log/info "chart-config" config data panel tab)
  (let [data-or-config [[config "config"]
                        [data "data"]]]
    [:div.chart-config {:style {:width "100%" :height "100%"}}
     [navbar/navbar data-or-config [panel]]
     [rc/scroller :src (rc/at)
      :v-scroll :auto
      :height "95%"
      :child [tab-panel/tab-panel {:extra-classes             :is-fluid
                                   :subscribe-to-selected-tab [tab]}

              [tab-panel/sub-panel {:panel-id config}
               config-panel]

              [tab-panel/sub-panel {:panel-id data}
               data-panel]]]]))


(defn path->string [& path]
  (->> path
    flatten
    (remove nil?)
    (map str)
    (map #(clojure.string/replace % #":" ""))
    (map #(clojure.string/replace % #"/" "."))
    (map #(clojure.string/replace % #" " "-"))
    (clojure.string/join ".")))


(defn path->keyword [& path]
  (->> path
    path->string
    keyword))


(defn string->keyword [s]
  (-> s
    str
    (clojure.string/replace #":" "")
    keyword))


(defn- resolve-subscription
  "resolve a subscription.

  there are 2 types if subscriptions: REMOTE and LOCAL

  REMOTE subscriptions are designed to reach across the network and query data from the Server, while
  LOCAL subscriptions are designed to reach into the Re-frame 'APP-DB' at a certain path
  "
  [subs opts]
  (let [[target & _] subs]
    ;(log/info "resolve-subscription" subs "//" target)
    (if (= target :bh.subs/source)
      (re-frame/subscribe (reduce conj subs opts))
      (re-frame/subscribe (reduce conj [(path->keyword subs)] opts)))))


(defn resolve-value [value & opts]
  ;(log/info "resolve-value" value "//" opts
  ;  "// (path-kw)" (reduce conj [(path->keyword value)] opts)
  ;  "// (path-sub)" (reduce conj [(path->keyword value)] opts))

  (let [ret (cond
              (keyword? value) (re-frame/subscribe (reduce conj [(path->keyword value)] opts))
              (and (coll? value)
                (not (empty? value))
                (every? (or keyword? string?) value)) (resolve-subscription value opts)
              (instance? reagent.ratom.RAtom value) value
              (instance? reagent.ratom.Reaction value) value
              (instance? Atom value) value
              :else (r/atom value))]
    ;(log/info "resolve-value" value "//" opts "//" ret "//" (str @ret))
    ret))



(defn handle-change [value new-value]
  ;(log/info "handle-change" value "//" new-value)
  (cond
    (or (coll? value)
      (keyword? value)
      (string? value)) (re-frame/dispatch (conj value new-value))
    (instance? reagent.ratom.RAtom value) (reset! value new-value)
    (instance? Atom value) (reset! value new-value)
    :else ()))


(defn handle-change-path [value path new-value]
  ;(log/info "handle-change-path" value "//" path "//" new-value)

  (cond
    (or (coll? value)
      (keyword? value)
      (string? value)) (let [update-event (conj [(path->keyword value path)] new-value)]
                         ;(log/info "handle-change-path (update event)" update-event)
                         (re-frame/dispatch update-event))
    (instance? reagent.ratom.RAtom value) (swap! value assoc-in path new-value)
    (instance? Atom value) (swap! value assoc-in path new-value)
    :else ()))



(comment
  (do
    (def container-id "simple-multi-chart")
    (def component-id (path->keyword container-id "widget"))
    (def data [component-id :blackboard :topic.data])
    (def path [:data])
    (def old-data (atom {:metadata {:type :tabular,
                                    :id :name,
                                    :title "Tabular Data with Metadata",
                                    :fields {:name :string, :uv :number, :pv :number, :tv :number, :amt :number}},
                         :data [{:name "Page A", :uv 4000, :pv 2400, :tv 1500, :amt 2400}
                                {:name "Page B", :uv 3000, :pv 1398, :tv 1500, :amt 2210}
                                {:name "Page C", :uv 2000, :pv 9800, :tv 1500, :amt 2290}
                                {:name "Page D", :uv 2780, :pv 3908, :tv 1500, :amt 2000}
                                {:name "Page E", :uv 1890, :pv 4800, :tv 1500, :amt 2181}
                                {:name "Page F", :uv 2390, :pv 3800, :tv 1500, :amt 2500}
                                {:name "Page G", :uv 3490, :pv 4300, :tv 1500, :amt 2100}]}))
    (def value data)
    (def new-value (assoc-in (:data @old-data) [0 :uv] 10000)))



  (cond
    (or (coll? value)
      (keyword? value)
      (string? value)) (let [update-event (conj [(path->keyword value path)] new-value)]
                         ;(log/info "handle-change-path (update event)" update-event)
                         (re-frame/dispatch update-event))
    (instance? reagent.ratom.RAtom value) (swap! value assoc-in path new-value)
    (instance? Atom value) (swap! value assoc-in path new-value)
    :else ())

  (handle-change-path data [:data]
    (assoc-in (:data @old-data) [0 :uv] 10000))



  ())



(comment
  (def path [:uv :fill])
  (def value [:dummy])

  (path->keyword value path)
  (conj [(path->keyword value path)] "#000000")

  (->>
    (re-frame/subscribe [:coverage-plan-demo.component.blackboard.topic.current-time])
    deref
    str)

  ())



(comment
  (path->string "one" "two" "three/dummy")
  (path->keyword "one" "two" "three/dummy")

  (path->keyword :area-chart-demo.area-chart :grid nil)
  (path->string :area-chart-demo.area-chart :grid nil)


  (path->keyword :topic/layers)
  (path->keyword [:topic/layers])

  (apply conj [:containers]
    (map path->keyword [:blackboard :topic/layers]))

  ())



