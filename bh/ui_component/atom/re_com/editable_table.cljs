(ns bh.ui-component.atom.re-com.editable-table
  (:require [bh.ui-component.utils.example-data :as ex]
            [bh.ui-component.utils.helpers :as h]
            [re-com.core :as rc]
            [re-com.util :refer [px]]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.atom.re-com.editable-table")


(def sample-data (r/atom ex/tabular-data))
(def meta-sample-data (r/atom ex/meta-tabular-data))
(def meta-coc-sample-data (r/atom (assoc ex/meta-tabular-data
                                    :c-o-c ex/default-coc)))


(defn- cell-click [[dataset editing-cell editing-cell-content] [rowidx colidx] k v]
  ;(log/info "cell clicked: " rowidx colidx k v)
  (if (and (= -1 colidx) (= -1 rowidx))
    (reset! dataset (into [] (sort-by v @dataset)))
    (do
      (reset! editing-cell {:row rowidx :col colidx k v})
      (reset! editing-cell-content (str v)))))


(defn- edit-comp [[dataset editing-cell editing-cell-content]]
  [:div#editable {:style {:display        "inline-block"
                          :vertical-align "middle"
                          :width          (px 195)
                          :height         (px 30)}}
   [:div#input {:style {:display        "inline-block"
                        :vertical-align "middle"}}
    [rc/input-text :src (rc/at)
     :model editing-cell-content
     :width (px 135)
     :height (px 25)
     :style {:align-items "bottom"}
     :change-on-blur? false
     :on-change #(do
                   ;(log/info "edit-comp" %)
                   (reset! editing-cell-content %))]]
   [:div#check {:style {:display        "inline-block"
                        :vertical-align "middle"
                        :padding        (px 2)}}
    [rc/md-icon-button :src (rc/at)
     :style {:display "inline-block"}
     :md-icon-name "zmdi-check"
     :size :smaller
     :on-click #(do
                  ;(log/info "check" [(:row @editing-cell) (key (last @editing-cell))] "//" @editing-cell-content)
                  (swap! dataset assoc-in [(:row @editing-cell)
                                           (key (last @editing-cell))]
                    @editing-cell-content)
                  (reset! editing-cell nil)
                  (reset! editing-cell-content nil))]]
   [:div#cancel {:style {:display        "inline-block"
                         :vertical-align "middle"
                         :padding        (px 2)}}
    [rc/md-icon-button :src (rc/at)
     :style {:display "inline-block"}
     :md-icon-name "zmdi-delete"
     :size :smaller
     :on-click #(do (reset! editing-cell-content nil)
                    (reset! editing-cell nil))]]])


(defn- span-with-border [{:keys [dataset
                                 editing-cell editing-cell-content
                                 rowidx colidx
                                 colname
                                 name
                                 background height width font-size]}]
  (let [is-editing? (r/atom (= [(:row @editing-cell)
                                (:col @editing-cell)]
                              [rowidx colidx]))]

    ;(log/info "span-with-border" [rowidx colidx] name colname)

    (if (and @is-editing?
          (not (= colidx -1)))
      [edit-comp [dataset editing-cell editing-cell-content]]
      [:span {:style    {:position         "static"
                         :width            (px width)
                         :height           (px height)
                         :border-radius    "2px"
                         :border           "solid grey 2px"
                         :vertical-align   "middle"
                         :background-color background
                         :display          "inline-block"
                         :text-align       "center"
                         :white-space      "nowrap"
                         :overflow         "hidden"
                         :text-overflow    "ellipsis"
                         :color            "black"
                         :font-size        font-size
                         :cursor           "pointer"}
              :on-click #(cell-click
                           [dataset editing-cell editing-cell-content]
                           [rowidx colidx] colname name)}
       (str name)])))


(defn- build-header [[dataset editing-cell editing-cell-content]
                     height vals]
  ;(log/info "build-header" vals)

  [:div {:class "headers"
         :style {:display    "inline-block"
                 :text-align "center"}}
   (doall
     (map-indexed
       (fn [idx v]
         ^{:key [idx]} [span-with-border {:name                 v
                                          :rowidx               -1 :colidx -1
                                          :font-size            20
                                          :background           "#60A0D8"
                                          :height               height :width 195
                                          :dataset              dataset
                                          :editing-cell         editing-cell
                                          :editing-cell-content editing-cell-content}])
       vals))])


(defn- build-row [[dataset editing-cell editing-cell-content]
                  row_height row_index row]

  ;(log/info "build-row" row)

  (let [values (vals row)
        ks     (into [] (keys row))]
    [:div {:class (str "row" row_index)
           :style {:display    "inline-block"
                   :text-align "center"}}
     (doall
       (map-indexed
         (fn [idx v]
           ^{:key [row_index idx (get ks idx)]}
           [span-with-border {:rowidx               row_index :colidx idx
                              :colname              (get ks idx)
                              :name                 v
                              :font-size            14
                              :background           "#ffffff" :height row_height :width 195
                              :dataset              dataset
                              :editing-cell         editing-cell
                              :editing-cell-content editing-cell-content}]) values))]))


(defn- table* [& {:keys [data width height max-rows]}]
  ; the apply/merge/map below is to stringify all the values so sorting still works once a value is edited
  ; this will need more modification/thought once we move beyond strings/ints in the table cells
  (let [dataset                   (r/atom
                                    (->> @data
                                      (map #(apply merge (map (fn [[k v]]
                                                                {k (str v)}) %)))
                                      (into [])))
        editing-cell              (r/atom nil)
        editing-cell-content      (r/atom nil)

        light-blue                "#d860a0"
        blue                      "#60A0D8"
        gold                      "#d89860"
        green                     "#60d898"
        white                     "#ffffff"

        fib-ratio                 0.618                     ;; fibonacci ratios to make the visuals look pretty
        unit-50                   50                        ;; base for fibonacci calulations
        unit-31                   (js/Math.round (* unit-50 fib-ratio))  

        num-rows                  (or max-rows 5)
        row-height                unit-31
        total-row-height          (* num-rows row-height)

        width-of-main-row-content (js/Math.round (/ total-row-height fib-ratio))]
        ;dummy-rows                (r/atom (mapv #(hash-map :id %1) (range num-rows)))]
    (fn []

      ;(log/info "table* INSIDE" data "//" @dataset)

      [rc/v-table :src (rc/at)
       :model dataset
       :max-width "inherit"

       ;; Data Rows (section 5)
       ;:row-renderer            (fn [_row_index, _row] [box-with-border {:name (str (:id _row)) :background light-blue :height row-height :width width-of-main-row-content}])
       :row-renderer (partial build-row [dataset editing-cell editing-cell-content] row-height)
       :row-content-width 1000
       :row-height row-height
       :max-row-viewport-height total-row-height            ;; force a vertical scrollbar

       ;; row header/footer (sections 2,8)
       ;:row-header-renderer     (fn [_row-index, _row] [box-with-border {:name ":row-header-renderer " :background green :height unit-31 :width unit-121}])
       ;:row-footer-renderer     (fn [_row-index, _row] [box-with-border {:name ":row-footer-renderer"  :background green :height unit-31 :width unit-121}])

       ;; column header/footer (sections 4,6)
       ;:column-footer-renderer  (fn [] [edit-comp])
       ;:column-footer-height    unit-50

       ;; 4 corners (sections 1,3,7,9)
       ;:top-left-renderer       (fn [] [box-with-border {:name ":top-left-renderer"     :background white  :height unit-50 :width unit-121}])
       ;:bottom-left-renderer    (fn [] [box-with-border {:name ":bottom-left-renderer"  :background white  :height unit-50 :width unit-121}])
       ;:top-right-renderer      (fn [] [box-with-border {:name ":top-right-renderer"    :background white  :height unit-50 :width unit-121}])
       ;:bottom-right-renderer   (fn [] [box-with-border {:name ":bottom-right-renderer" :background white  :height unit-50 :width unit-121}])

       :column-header-renderer (fn [] [build-header
                                       [dataset editing-cell editing-cell-content]
                                       (+ row-height 10)
                                       (into (keys (get @data 0)))])
       :column-header-height 40])))


(defn- non-meta-table [& {:keys [data max-rows width height cell-style-fn
                                 on-click-row-fn row-line-color]}]
  (let [d (h/resolve-value data)]
    ;(log/info "non-meta-table" data "//" @d)
    (fn []
      [table*
        :data d
        :max-rows max-rows
        :width width
        :height height
        :row-line-color row-line-color
        :on-click-row on-click-row-fn
        :cell-style-fn cell-style-fn])))


(defn- meta-table [& {:keys [data max-rows width height cell-style-fn
                             on-click-row-fn row-line-color]}]
  (let [d (h/resolve-value data)]
    (fn []

      (let [coc? (r/atom false)]
        ;(log/info "meta-table" data "//" @d "//" (:data @d))

        [:div.card {:style {:width  (or width "100%") :height (or height "100%")
                            :margin :auto}}
         [:h3 (-> @d :metadata :title)]
         [rc/h-box :src (rc/at)
          :gap "2px"
          :children [[table*
                      :data (h/resolve-value (if (:data @d) (:data @d) []))
                      :max-rows max-rows
                      :width width
                      :height height
                      :row-line-color row-line-color
                      :on-click-row on-click-row-fn
                      :cell-style-fn cell-style-fn]
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
                                          :max-rows 3]]]])]]]))))


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




; Testing out row building
(comment
  (def data sample-data)
  ;(sort-by :pv @dataset)
  (map #(map (fn [k v] (str v)) %) @data)
  (into [] (map #(apply merge (map (fn [[k v]] {k (str v)}) %)) @dataset))

  (keys (get @data 2))
  (mapv #(merge {:id (:name %)} %) @data)
  (mapv #(clojure.set/rename-keys % {:name :id}) @data)

  (map (fn [k v] [box-with-border {:name       (str v)
                                   :background "#d860a0"
                                   :height     30
                                   :width      50}]) (get @data 0))
  (build-row 1 (get @data 0))


  (def row {:id "Page A", :kp 2000, :uv 4000, :pv 2400, :amt 2400})
  (def values (vals row))
  (-> (into [] (map #(vector box-with-border {:name (str %) :background "#d860a0" :height 30 :width 500}) values))
    (with-meta {:key (rand-int 30)}))
  ^{:key (rand-int 30)}

  @is-editing?
  @dataset
  (assoc-in @dataset [(:row @editing-cell) (key (last @editing-cell))] @editing-cell-content)
  (swap! dataset update-in [(:row @editing-cell) (key (last @editing-cell))] @editing-cell-content)


  ())



