(ns bh.ui-component.tabbed-pane.utils
  (:require [taoensso.timbre :as log]
            [re-frame.core :as re-frame]
            [woolybear.packs.tab-panel :as tab-panel]

            [bh.events :as events]))


(defn init-tabbed-panel [base-id initial-value]
  ;(log/info "init-tabbed-panel" base-id)

  (let [formal-id (keyword base-id)
        data-path [formal-id :tab-panel]
        config-id (keyword base-id "config")
        data-id (keyword base-id "data")
        db-id (keyword "db" base-id)
        tab-id (keyword base-id "tab-panel")
        value-id (keyword base-id "value")
        init-db {:tab-panel (tab-panel/mk-tab-panel-data
                              data-path initial-value)}]

    (re-frame/reg-sub
      db-id
      (fn [db _]
        (formal-id db)))

    (re-frame/reg-sub
      tab-id
      :<- [db-id]
      (fn [navbar]
        (:tab-panel navbar)))

    (re-frame/reg-sub
      value-id
      :<- [tab-id]
      (fn [tab-panel]
        (:value tab-panel)))

    (re-frame/dispatch-sync [::events/init-locals formal-id init-db])))