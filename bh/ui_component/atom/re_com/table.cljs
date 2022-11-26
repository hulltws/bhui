(ns bh.ui-component.atom.re-com.table
  (:require [bh.ui-component.utils.helpers :as h]
            [re-com.core :as rc]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.table")


(defn table-column-headers [data rows width height]
  (let [d          (apply set (map keys data))
        col-count  (count d)
        col-width  (max 80 (- (/ (or width 400) col-count) 5))
        row-height (max 50 (/ (or height 400) (+ 2 (or rows 5))))]
    (->> d
      (map (fn [k]
             {:id    k :header-label (name k) :row-label-fn k
              :width col-width :height row-height}))
      (into []))))


(defn- table* [& {:keys [data max-rows width height cell-style-fn
                         on-click-row-fn row-line-color]}]

  ;(log/info "table-star" @data)

  (if (empty? @data)

    [rc/alert-box :src (rc/at)
     :alert-type :info
     :heading "Waiting for data"]

    [rc/simple-v-table :src (rc/at)
     :model data
     :columns (table-column-headers @data 5 (or width 200) (or height))
     :max-rows (or max-rows (count @data))
     :table-row-line-color (or row-line-color "#00fff0")
     :on-click-row (or on-click-row-fn #())
     :cell-style (or cell-style-fn #())
     :parts {:simple-wrapper {:style {:border false}}}]))


(defn- non-meta-table [& {:keys [data max-rows width height cell-style-fn
                                 on-click-row-fn row-line-color]}]

  (let [d (h/resolve-value data)]
    (fn []
      ;(log/info "non-meta-table" data "//" @remote)
      [:div {:style {:width  (or width "300px") :height (or height "250px")
                     :margin :auto}}
       [table*
        :data d
        :max-rows max-rows
        :width width
        :height height
        :row-line-color (or row-line-color "#00fff0")
        :on-click-row (or on-click-row-fn #())
        :cell-style-fn (or cell-style-fn #())]])))


(defn meta-table [& {:keys [data max-rows width height cell-style-fn
                            on-click-row-fn row-line-color]}]
  (let [d    (h/resolve-value data)
        coc? (r/atom false)]

    (fn []
      ;(log/info "meta-table (inner)" data "//" @d)

      [:div.card {:style {:width  (or width "90%") :height (or height "100%")
                          :margin :auto}}
       [rc/h-box :src (rc/at)
        :gap "2px"
        :children [[table*
                    :data (h/resolve-value (if (:data @d) (:data @d) []))
                    :max-rows (or max-rows (count (:data @d)))
                    :width width
                    :height height
                    :row-line-color (or row-line-color "#00fff0")
                    :on-click-row (or on-click-row-fn #())
                    :cell-style-fn (or cell-style-fn #())]
                   (when (seq (:c-o-c @d))
                     [:div
                      [rc/popover-anchor-wrapper :src (rc/at)
                       :showing? coc?
                       :position :below-center
                       :anchor [rc/md-icon-button
                                :md-icon-name "zmdi zmdi-badge-check"
                                :tooltip "view chain-of-custody"
                                :on-click #(swap! coc? not)]
                       :popover [rc/popover-content-wrapper :src (rc/at)
                                 :title "Chain-of-Custody"
                                 ;:no-clip? true
                                 :body [table*
                                        :data (h/resolve-value (if (:c-o-c @d) (:c-o-c @d) []))
                                        :max-rows 3]]]])]]])))


(defn table [& {:keys [data max-rows width height cell-style-fn
                       on-click-row-fn row-line-color]}]

  (let [d (h/resolve-value data)]
    ;(log/info "table" data "//" @d "//" (:data @d))
    (if (:metadata @d)
      [meta-table
       :data data
       :max-rows max-rows
       :width width
       :height height
       :cell-style-fn cell-style-fn
       :on-click-row-fn on-click-row-fn
       :row-line-color row-line-color]
      [non-meta-table
       :data data
       :max-rows max-rows
       :width width
       :height height
       :cell-style-fn cell-style-fn
       :on-click-row-fn on-click-row-fn
       :row-line-color row-line-color])))


(def meta-data {:rc/table {:component table
                           :ports     {:data :port/sink}}})


(re-frame/dispatch-sync [:register-meta meta-data])




(comment

  (def value [{:generated-by "dummy.data-source.targets",
               :at           #inst "2022-03-17T20:40:28.006-00:00",
               :signature    "53dbe964-a4cc-4c36-965a-3e03fdd84b53"}])
  (def opts nil)

  (and (coll? value)
    (not (empty? value))
    (every? keyword? value))
  (instance? reagent.ratom.RAtom value)
  (cond
    ; TODO: can this be converted to (apply concat...)? (see https://clojuredesign.club/episode/080-apply-as-needed/)
    (and (coll? value)
      (not (empty? value))
      (every? keyword? value)) (re-frame/subscribe (reduce conj value opts))
    (instance? reagent.ratom.RAtom value) value
    (instance? Atom value) value
    :else (r/atom value))

  (seq (:c-o-c {:c-o-c []}))

  (def data [:bh.subs/source :source/targets])
  (def d (h/resolve-value data))

  (seq (:c-o-c @d))

  (def some-code {:dummy  {:one :port/sink :alpha :port/sink}
                  :dummy2 {:two :port/source}})

  (str some-code)

  (clojure.string/join "\n" (clojure.string/split (str some-code) #","))

  ())
