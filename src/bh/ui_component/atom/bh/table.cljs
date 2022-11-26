(ns bh.ui-component.atom.bh.table
  (:require [bh.ui-component.utils.example-data :as ex]
            [bh.ui-component.utils.helpers :as h]
            [re-com.core :as rc]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(def sample-data ex/tabular-data)
(def sample-meta-data ex/meta-tabular-data)
(def sample-meta-coc-data (assoc ex/meta-tabular-data
                            :c-o-c ex/default-coc))
(def random-data ex/random-tabular-data)
(def random-data-meta ex/random-meta-tabular-data)


(defn- table* [& {:keys [data max-rows width height cell-style-fn
                         on-click-row-fn row-line-color]}]

  ;(log/info "table(star)" data)

  (if (empty? data)

    [rc/alert-box :src (rc/at)
     :alert-type :info
     :heading "Waiting for data"]

    (let [header (keys (first data))
          body   data]

      ;(log/info "table(STAR) INSIDE" header "//" body)

      [:div.table-container {:style {:width (or width "100%")
                                     :height (or height "100%")
                                     :overflow  :scroll}}
       [:table.table.is-hoverable {:style {:width "100%" :height "100%"}}
        [:thead {:style {:position :sticky :top 0 :background :darkslategray}}
         [:tr
          (doall (for [[idx h] (map-indexed vector header)]
                   (do
                     ;(log/info "header" idx h)
                     ^{:key idx} [:th {:style {:color :white}} (str h)])))]]

        [:tbody
         (doall
           (for [[idx b] (map-indexed vector body)]
             (do
               ;(log/info "body" idx b)
               ^{:key idx} [:tr (for [key header]
                                  (do
                                    ;(log/info "cell" key)
                                    ^{:key key} [:td (str (get b (keyword key)))]))])))]]])))


(defn- non-meta-table [& {:keys [data max-rows width height cell-style-fn
                                 on-click-row-fn row-line-color]}]
  (let [d (h/resolve-value data)]
    ;(log/info "non-meta-table" data "//" @d)
    [table*
     :data @d
     :max-rows max-rows
     :width width
     :height height
     :row-line-color row-line-color
     :on-click-row on-click-row-fn
     :cell-style-fn cell-style-fn]))


(defn- meta-table [& {:keys [data max-rows width height cell-style-fn
                             on-click-row-fn row-line-color]}]
  (let [d (h/resolve-value data)]
    (fn []

      ;(log/info "meta-table" data "//" @d "//" (:data @d))

      (let [coc? (r/atom false)]
        [:div.card {:style {:width  (or width "90%") :height (or height "100%")
                            :margin :auto}}
         [:div.card-header
          [:div.card-header-title
           [rc/h-box :src (rc/at)
            :width "100%"
            :justify :between
            :children [(-> @d :metadata :title)
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
                                     :body [table*
                                            :data (:c-o-c @d)
                                            :width "400px"
                                            :max-width "400px"
                                            :max-rows 3]]]])]]]]
         [:div.card-content
          [table*
           :data (if (:data @d) (:data @d) [])
           :max-rows max-rows
           :width width
           :height height
           :row-line-color row-line-color
           :on-click-row on-click-row-fn
           :cell-style-fn cell-style-fn]]]))))



(defn table [& {:keys [data max-rows width height cell-style-fn
                       on-click-row-fn row-line-color]}]

  (let [d (h/resolve-value data)]
    ;(log/info "table" data "//" @d "//" (:data @d) "//" (:metadata @d))
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



(def meta-data {:bh/table {:component table
                           :ports     {:data :port/sink}}})


(re-frame/dispatch-sync [:register-meta meta-data])






(comment
  (def value sample-data)

  (def value [])

  ;(let [ret (cond
  ;            (and (coll? value)
  ;              (not (empty? value))
  ;              (every? keyword? value)) (re-frame/subscribe value)
  ;            (instance? reagent.ratom.RAtom value) value
  ;            (instance? Atom value) value
  ;            :else (r/atom value))]
  ;  ;(log/info "resolve-value" value "//" ret "//" (str @ret))
  ;  ret)

  (empty? @(h/resolve-value []))

  (def d (h/resolve-value ex/meta-tabular-data))

  (keys (first (:data @d)))
  (:data @d)

  (def body (:data @d))
  (map-indexed vector body)


  (def d (h/resolve-value ex/tabular-data))
  (keys (first @d))

  (def body @d)
  (map-indexed vector body)


  (h/resolve-value [:bh.subs/source :source/targets])
  (h/resolve-value [:bh.subs/source :source/targets] :title)

  (re-frame/subscribe [:bh.subs/source :source/targets :data])

  (get-in @re-frame.db/app-db [:sources :source/targets :data])

  (get-in @re-frame.db/app-db (reduce conj [:sources :source/targets] '(:data)))

  (or (get-in @re-frame.db/app-db (reduce conj [:sources :dummy] '(:data))) [])

  (h/resolve-value [])

  ())