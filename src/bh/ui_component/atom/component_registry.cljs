(ns bh.ui-component.atom.component-registry
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [taoensso.timbre :as log]))



(rf/reg-event-db
  :register-meta
  (fn-traced [db [_ meta-data]]
    ;(log/info ":register-meta" meta-data)
    (if (:meta-data-registry db)
      (update db :meta-data-registry conj meta-data)
      (assoc db :meta-data-registry meta-data))))


(rf/reg-sub
  :meta-data-registry
  (fn [db _]
    (:meta-data-registry db)))



