(ns bh.ui-component.utils
  (:require [bh.ui-component.utils.color :as c]
            [bh.ui-component.utils.helpers :as h]
            [bh.ui-component.utils.locals :as l]
            [bh.ui-component.utils.container :as ctnr]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [taoensso.timbre :as log]))




;;;;;;;;;;;;;
;
; helpers
;

(def default-pub-sub {:pub [] :sub [] :container ""})




(def h-wrap {:-webkit-flex-flow "row wrap"
             :flex-flow         "row wrap"})


(def v-wrap {:-webkit-flex-flow "column wrap"
             :flex-flow         "column wrap"})


(defn config-tab-panel [component-id]
  (h/config-tab-panel component-id))


(defn component-id []
  (h/component-id))


(defn chart-config [v data-panel config-panel]
  (h/chart-config v data-panel config-panel))


(defn path->string [& path]
  (apply h/path->string path))


(defn path->keyword [& path]
  (apply h/path->keyword path))


;;;;;;;;;;;;;
;
; container locals
;

(defn init-container-locals [container-id locals-and-defaults]
  (l/init-container-locals container-id locals-and-defaults))


(defn subscribe-local [container-id value-path]
  (l/subscribe-local container-id value-path))


(defn resolve-subscribe-local [container-id value-path]
  (l/resolve-subscribe-local container-id value-path))


(defn dispatch-local [container-id value-path new-val]
  (l/dispatch-local container-id value-path new-val))


(defn build-subs [container-id local-config]
  (l/build-subs container-id local-config))


(defn resolve-sub [subs path]
  (l/resolve-sub subs path))


(defn init-container [container-id]
  (ctnr/init-container container-id))


(defn subscribe-to-container [container-id component-path]
  (ctnr/subscribe-to-container container-id component-path))


(defn publish-to-container [container-id component-path new-val]
  (ctnr/publish-to-container container-id component-path new-val))


(defn override-subs [container-id local-subs subs]
  (ctnr/override-subs container-id local-subs subs))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; Remote DataSource Support
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; region

(re-frame/reg-event-db
  :events/init-remote-data-source
  (fn-traced [db [_ data-source-id]]
    (log/info ":events/init-remote-data-source" data-source-id)))


(defn init-data-source [data-source-id]
  ;(log/info ":events/init-remote-data-source" data-source-id)
  (re-frame/dispatch [:events/init-remote-data-source data-source-id]))


(defn subscribe-data-source [data-source-id]
  ;(log/info "subscribe-data-source" data-source-id)
  (re-frame/subscribe [:data-sources (path->keyword data-source-id)]))



;; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; Rich Comments
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; region

; how well does relative-luminance work?
(comment
  (do
    (def black {:r 0 :g 0 :b 0 :a 1.0})
    (def white {:r 255 :g 255 :b 255 :a 1.0})
    (def red {:r 255 :g 0 :b 0 :a 1.0})
    (def green {:r 0 :g 255 :b 0 :a 1.0})
    (def blue {:r 0 :g 0 :b 255 :a 1.0}))

  (relative-luminance black)
  (relative-luminance white)
  (relative-luminance red)
  (relative-luminance green)
  (relative-luminance blue)

  ())


; how well does best-text-color work?
(comment
  (do
    (def black {:r 0 :g 0 :b 0 :a 1.0})
    (def white {:r 255 :g 255 :b 255 :a 1.0})
    (def red {:r 255 :g 0 :b 0 :a 1.0})
    (def green {:r 0 :g 255 :b 0 :a 1.0})
    (def blue {:r 0 :g 0 :b 255 :a 1.0}))

  (best-text-color black)
  (best-text-color white)
  (best-text-color red)
  (best-text-color green)
  (best-text-color blue)

  ())


; need to mix the container-id in with the path "inside" the container's hash-map
(comment
  (def app-db
    {:containers {:<guid-1>   {:tab-panel  {:value     :<guid-1>/dummy
                                            :data-path [:<guid-1> :tab-panel]}
                               :some-value "value"
                               :grid       {:include false}
                               :x-axis     {:include false}}
                  :catalog    {:tab-panel {:value     :catalog/atoms
                                           :data-path [:catalog :tab-panel]}}
                  :line-chart {:tab-panel {:value     :line-chart/config
                                           :data-path [:line-chart :tab-panel]}
                               :grid      {:include true :strokeDasharray {:d 3 :g 3}
                                           :stroke  "#ffffff"}
                               :x-axis    {:include     true :dataKey ""
                                           :orientation :bottom :scale "auto"}
                               :y-axis    {:include       true :dataKey "" :orientation
                                           :bottom :scale "auto"}
                               :legend    {:include true :layout :horizontal
                                           :align   :center :verticalAlign :bottom}
                               :tooltip   {:include true}}}})


  (def container-id "<guid-1>")
  (def path [:some-value])
  (def path [:tab-panel :value])

  (defn subscribe-local [container-id [a & more]]
    (let [p (path->keyword container-id (str (name a)
                                          (when more
                                            (str "." (clojure.string/join "." (map name more))))))]
      p))
  ;(re-frame/subscribe p)))

  (let [d (subscribe-local :line-chart [:grid :strokeDasharray :d])]
    d)
  (path->keyword :line-chart)

  (subscribe-local container-id [:some-value])
  (subscribe-local :line-chart [:tab-panel :value])
  (subscribe-local :line-chart [:grid :strokeDasharray :d])

  ())


; how do we build all the cascading subscriptions for the container's locals?
; rocky-road just uses a single [containers "locals" container-id :some-value]...
; so it doesn't HAVE cascaded subscriptions in the first place
(comment

  (def container-locals {:tab-panel  {:value     :<guid-1>/dummy
                                      :data-path [:<guid-1> :tab-panel]}
                         :some-value "value"
                         :grid       {}
                         :x-axis     {}})

  ; NOTE 1: does ':data-path' need the :containers prefix to work? PROBABLY

  ; NOTE 2: container-locals is both the structure AND the initial value


  ; THE GOAL:
  ;
  ;      (init-container-locals-locals "container-1" container-locals)
  ;
  ; this (1) builds all the subscriptions AND (2) loads the initial data into the app-db
  ; at the correct level


  ;; region ; set initial values into the app-db:
  (defn load-local-values [container-id values]
    (let [target (path->keyword container-id)
          path   [:events/init-container-locals target values]]
      (re-frame/dispatch-sync path)))

  (load-local-values "<guid-1>" container-locals)
  (load-local-values "<guid-2>" container-locals)

  ;; endregion

  ;; region ; building the subscriptions

  ; let's start with hand-crafted, artisanal subscriptions
  ; sub-container
  (re-frame/reg-sub
    :containers/<guid-1>
    :<- [:containers]
    (fn [containers _]
      (:<guid-1> containers)))
  @(re-frame/subscribe [:containers])
  (->> @(re-frame/subscribe [:containers/<guid-1>])
    keys)

  ; sub-some-value
  (re-frame/reg-sub
    :<guid-1>/some-value
    :<- [:containers/<guid-1>]
    (fn [containers _]
      (:some-value containers)))
  @(re-frame/subscribe [:<guid-1>/some-value])

  ; sub-tab-panel
  (re-frame/reg-sub
    :<guid-1>/tab-panel
    :<- [:containers/<guid-1>]
    (fn [containers _]
      (:tab-panel container)))
  @(re-frame/subscribe [:containers/<guid-1>])
  @(re-frame/subscribe [:<guid-1>/tab-panel])

  ; sub-tab-panel-value
  ;    see `subscribe-local` above
  (re-frame/reg-sub
    :<guid-1>/tab-panel.value
    :<- [:<guid-1>/tab-panel]
    (fn [tab-panel _]
      (:value tab-panel)))
  @(re-frame/subscribe [:<guid-1>/tab-panel.value])

  ;; endregion

  ;; region ; subscribing to locals (chart around re-frame/subscribe)
  (defn subscribe-local [container-id [a & more :as path]]
    (let [p (path->keyword container-id (str (name a)
                                          (when more
                                            (str "." (clojure.string/join "." (map name more))))))]
      ;(log/info "subscribe-local" container-id path p)
      (re-frame/subscribe [p])))

  ; let's spell out what we needed to build these subscriptions
  (def sub-container ["<guid-1>"])                             ; [(assume :containers) <container-id>]
  (def sub-some-value ["<guid-1>" [:some-value]])           ; [<container-id> <path>]
  (def sub-tab-panel ["<guid-1>" [:tab-panel]])             ; [container-id> <path>]
  (def sub-tab-panel-value ["<guid-1>" [:tab-panel :value]]) ; [<container-id> <path>]

  ; so 2 types:
  ;      "create-container-sub"        i.e., [<container-id>] (`:widget` is assumed)
  ;      "create-container-local-sub"  i.e., [<container-id> [<path>]]

  (path->keyword :containers "dummy.part-1.part-2")
  (path->keyword :containers ":dummy")
  (name :dummy)

  ;; endregion

  ;; region ; create all the subscriptions (by hand)
  (defn create-container-sub [container-id]
    (let [id (path->keyword container-id)
          w  (path->keyword :containers container-id)]
      (re-frame/reg-sub
        w
        :<- [:containers]
        (fn [containers _]
          ;(log/info w id)
          (get containers id)))))


  (defn create-container-local-sub [container-id [a & more]]
    (let [p   (path->keyword container-id (str (name a)
                                            (when more
                                              (str "." (clojure.string/join "." (map name more))))))
          dep (if more
                (path->keyword container-id
                  (str (name a)
                    (when (seq (drop-last [:value]))
                      (str "." (clojure.string/join "." (map name (drop 1 more)))))))
                (path->keyword :containers container-id))]
      ;(log/info "create-container-local-sub" p dep more (if more (last more) a))
      (re-frame/reg-sub
        p
        :<- [dep]
        (fn [container _]
          ;(log/info p dep container (last more))
          (get container (if more (last more) a))))))


  (create-container-sub "<guid-1>")
  @(re-frame/subscribe [:containers/<guid-1>])

  (create-container-local-sub "<guid-1>" [:tab-panel])
  (create-container-local-sub "<guid-1>" [:tab-panel :value])
  (create-container-local-sub "<guid-1>" [:tab-panel :data-path])
  (create-container-local-sub "<guid-1>" [:some-value])
  (create-container-local-sub "<guid-1>" [:grid])
  (create-container-local-sub "<guid-1>" [:x-axis])

  @(subscribe-local :<guid-1> [:tab-panel])
  @(subscribe-local :<guid-1> [:tab-panel :value])
  @(subscribe-local :<guid-1> [:tab-panel :data-path])
  @(subscribe-local :<guid-1> [:some-value])
  @(subscribe-local :<guid-1> [:grid])
  @(subscribe-local :<guid-1> [:x-axis])


  @(re-frame/subscribe [:<guid-1>/tab-panel])

  (create-container-local-sub "<guid-1>" [:tab-panel :value])
  @(subscribe-local :<guid-1> [:tab-panel :value])

  ;; endregion

  ())


; now to figure out what subscriptions need to be built for a
; given container/initial-values-map
(comment
  (def container-id "<guid-1>")

  ; GOAL:
  ;
  ;   (init-container-locals-locals container-id container-locals)
  ;
  ; turn container-locals into:
  ;
  ;     {"<guid-1>" [[:tab-panel]                    => :<guid-1>/tab-panel
  ;                  [:tab-panel :value]             =>
  ;                  [:tab-panel :data-path]
  ;                  [:some-value]
  ;                  [:grid]
  ;                  [:grid :include]
  ;                  [:grid :strokeDasharray]
  ;                  [:grid :strokeDasharray :dash]  => :<guid-1>/grid.strokeDasharray.dash
  ;                  [:grid :strokeDasharray :space] => :<guid-1>/grid.strokeDasharray.space
  ;                  [:x-axis]
  ;                  [:x-axis :include]
  ;                  [:set-of-data]}
  ;
  ; which can then be processed by
  ;    (create-container-sub) and (create-container-local-sub)
  ;

  (def container-locals {:tab-panel   {:value     :<guid-1>/dummy
                                       :data-path [:<guid-1> :tab-panel]}
                         :some-value  "value"
                         :grid        {:include         true
                                       :strokeDasharray {:dash 3 :space 3}}
                         :x-axis      {:include     true
                                       :orientation :bottom}
                         :set-of-data {}})

  (reduce + 0 [1 2 3 4 5])

  (loop [a 0
         c [1 2 3 4 5]]
    (if (empty? c)
      a                                                     ; done!
      (recur (+ a (first c)) (rest c))))


  (defn process-locals [a r t]
    (println "process-locals" a r t)
    (loop [accum a
           root  r
           tree  t]
      (println "process" tree root accum)
      (if (empty? tree)
        (do
          (println "result" accum)
          accum)
        (let [[k v] (first tree)]
          (println "let" k v)
          (recur (if (map? v)
                   (do
                     (println "branch" v (if root
                                           (if (vector? root)
                                             (conj root k)
                                             [root k])
                                           [k]) accum)
                     (as-> accum x
                       ; add this root to the accum
                       (conj x (if root
                                 (if (vector? root)
                                   (conj root k)
                                   [root k])
                                 [k]))
                       ; now process the sub-tree
                       (apply conj x (process-locals []
                                       (if root
                                         (if (vector? root)
                                           (conj root k)
                                           [root k])
                                         k)
                                       v))))
                   (do
                     (println "leaf" root k accum)
                     (conj accum (if root
                                   (if (vector? root)
                                     (conj root k)
                                     [root k])
                                   [k]))))
            root
            (rest tree))))))

  ;; region ; example-based tests
  (= (process-locals [] nil {:a 1 :b 2})
    [[:a] [:b]])

  (= (process-locals [] nil {:a 1 :b 2 :c 3})
    [[:a] [:b] [:c]])

  (= (process-locals [] nil {:a 1 :b {:c 2 :d 3}})
    [[:a] [:b] [:b :c] [:b :d]])

  (= (process-locals [] nil {:a 1 :b {:c 2} :d {:e 3}})
    [[:a] [:b] [:d] [:b :c] [:d :e]])

  (= (process-locals [] nil {:a 1 :b {:c 2 :d {:e 3 :f 4}}})
    [[:a] [:b] [:b :c] [:b :d] [:b :d :e] [:b :d :f]])

  (= (process-locals [] nil {:a 1 :b {:c 2 :d {:e 3 :f {:g [2 4] :h {:i 100}}}}})
    [[:a] [:b] [:b :c] [:b :d] [:b :d :e] [:b :d :f]
     [:b :d :f :g] [:b :d :f :h] [:b :d :f :h :i]])


  (= (process-locals [] nil container-locals)
    [[:tab-panel]
     [:tab-panel :value]
     [:tab-panel :data-path]
     [:some-value]
     [:grid]
     [:grid :include]
     [:grid :strokeDasharray]
     [:grid :strokeDasharray :dash]
     [:grid :strokeDasharray :space]
     [:x-axis]
     [:x-axis :include]
     [:x-axis :orientation]
     [:set-of-data]])

  ;; endregion


  ())


; building the complete set of subscriptions and event-handlers for a 'container'
; and then testing them out
(comment
  (do
    (def container-id "<guid-1>")
    (def container-locals {:tab-panel   {:selected-panel :<guid-1>/dummy
                                         :data-path      [:<guid-1> :tab-panel]}
                           :some-value  "value"
                           :grid        {:include         true
                                         :strokeDasharray {:dash 3 :space 3}}
                           :x-axis      {:include     true
                                         :orientation :bottom}
                           :set-of-data #{}}))

  (conj [1 2 3] [4 5])
  (apply conj [1 2 3] [4 5])

  ; set everything up
  (init-container-locals container-id container-locals)

  ;; region ; try out some subscriptions
  (= @(subscribe-local container-id [:tab-panel])
    [])
  @(subscribe-local container-id [:tab-panel :value])
  @(subscribe-local container-id [:tab-panel :data-path])
  @(subscribe-local container-id [:some-value])
  @(subscribe-local container-id [:grid])
  @(subscribe-local container-id [:grid :include])
  @(subscribe-local container-id [:grid :strokeDasharray])
  @(subscribe-local container-id [:grid :strokeDasharray :dash])
  @(subscribe-local container-id [:grid :strokeDasharray :space])
  @(subscribe-local container-id [:x-axis])
  @(subscribe-local container-id [:x-axis :include])
  @(subscribe-local container-id [:x-axis :orientation])
  @(subscribe-local container-id [:set-of-data])

  ;; endregion

  ;; region ; try out the event-handler (user the subscription above to see the updated value)
  (dispatch-local container-id [:grid :include] true)
  (dispatch-local container-id [:grid :include] false)
  (dispatch-local container-id [:grid :strokeDasharray :dash] 5)
  (dispatch-local container-id [:grid :strokeDasharray :space] 1)
  (dispatch-local container-id [:set-of-data] #{1 2 3 4 5})




  ;; endregion



  ())


; playing with subscriptions and events
(comment
  @(subscribe-local "line-chart-demo" [:line-chart-demo/tab-panel.value])
  (dispatch-local "line-chart-demo" [:tab-panel :value] :line-chart-demo/data)
  (dispatch-local "line-chart-demo" [:tab-panel :value] :line-chart-demo/config)

  (re-frame/dispatch [:line-chart-demo/tab-panel.value :line-chart-demo/config])
  (re-frame/dispatch [:line-chart-demo/tab-panel.value :line-chart-demo/data])


  ())


; how do we publish things to a "container"?
(comment
  (do (def db {:containers {:container {:blackboard {}}}})
      (def container-id :container)
      (def component-path [:chart-1 :data]))

  (get-in db [:containers container-id :blackboard])

  (-> db
    (update-in [:containers container-id :blackboard]
      assoc [:chart-1 :data] "new-val")
    (update-in [:containers container-id :blackboard]
      assoc [:chart-2 :data] "another-val"))

  ())


; turning a hash-map into a collection of vectors that are the key paths into all
; the data leaves
(comment
  (def local-config {:brush false,
                     :uv    {:include true, :stroke "#8884d8", :fill "#8884d8"},
                     :pv    {:include true, :stroke "#ffc107", :fill "#ffc107"},
                     :tv    {:include true, :stroke "#82ca9d", :fill "#82ca9d"},
                     :amt   {:include true, :stroke "#ff00ff", :fill "#ff00ff"}})


  (->> (process-locals [] nil local-config)
    (map (fn [path]
           (log/info "build-container-subs" container-id path)
           {path (subscribe-to-container container-id path)}))
    (into {}))

  (do
    (def widgcontainer-idet-id "multi-chart-demo/multi-chart")
    (def a :tv)
    (def more [:fill]))

  (compute-container-path container-id a more)




  ())


; building valid keyword for use in the app-db, subscriptions, events, etc
(comment
  (def path ["line-chart-demo" "line-chart" "tab-panel" "value"])
  (def path2 '("line-chart-demo" "line-chart" [:uv :fill]))

  (keyword (clojure.string/join "." path))

  (path->keyword "line-chart-demo" "line-chart" "tab-panel" "value")

  (flatten path2)
  (apply conj [] (flatten path2))

  (->> path2
    flatten
    (apply conj [])
    (map name)
    (clojure.string/join ".")
    keyword)

  (path->keyword "line-chart-demo" "line-chart" [:uv :fill])

  ())

;; endregion

