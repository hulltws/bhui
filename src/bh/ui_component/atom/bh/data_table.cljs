(ns bh.ui-component.atom.bh.data-table
  (:require [bh.ui-component.utils.example-data :as ex]
            [bh.ui-component.utils.helpers :as h]
            [reagent.core :as r]
            [taoensso.timbre :as log]

            ["fixed-data-table-2" :refer [Table Column DataCell]]))

(log/info "bh.ui-component.atom.bh.data-table")


(def sample-data ex/tabular-data)
(def sample-data-2 ["string 1" "string 2" "string 3"])


(defn- make-row-cell [data columnId]
  ;(log/info "make-row-cell" data "//" columnId)

  [:> Column {:header    (fn [] (r/as-element [:> DataCell (name columnId)]))
              :columnKey (name columnId)
              :cell      (fn [x]
                           (let [{:keys [rowIndex] :as all} (js->clj x :keywordize-keys true)]
                             (r/as-element [:> DataCell (columnId (nth data rowIndex))])))
              :width     200}])


(defn table [& {:keys [data config-data]}]

  (let [d (h/resolve-value data)
        c (h/resolve-value config-data)
        columns (->> @d first keys)]

    ;(log/info "table " columns)

    (into [:> Table {:rowHeight 30 :rowsCount (count @d) :width 500 :height 500 :headerHeight 50}]
      (doall
        (map #(make-row-cell @d %) columns)))))


