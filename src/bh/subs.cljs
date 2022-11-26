(ns bh.subs
  (:require
    [re-frame.core :as re-frame]))


(re-frame/reg-sub
  ::pub-sub-started?
  (fn [db]
    (:pub-sub-started? db)))


(re-frame/reg-sub
  :containers
  (fn [db]
    (:containers db)))


(re-frame/reg-sub
  ::source
  (fn [db [_ id]]
    (or (get-in db [:sources id]) [])))


(re-frame/reg-sub
  ::subscribed
  (fn [db [_ source]]
    (contains? (:subscribed db) source)))


(re-frame/reg-sub
  ::subscribe-error
  (fn [db _]
    (:subscribe-error db)))

