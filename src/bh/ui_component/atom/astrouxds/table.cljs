(ns bh.ui-component.atom.astrouxds.table
  (:require ["@astrouxds/react" :refer (RuxTable RuxTableHeader RuxTableHeaderRow RuxTableHeaderCell RuxTableBody RuxTableRow RuxTableCell)]))

(defn table [header row1 row2 row3 row4 row5]
   [:> RuxTable
    [:> RuxTableHeader
     [:> RuxTableHeaderRow
       (doall (for [h header]
                [:> RuxTableHeaderCell h]))]]
    [:> RuxTableBody
     (doall (for [r [row1 row2 row3 row4 row5]]
              [:> RuxTableRow {:selected false}
               (doall (for [c r]
                        [:> RuxTableCell c]))]))]])

