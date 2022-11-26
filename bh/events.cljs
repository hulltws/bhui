(ns bh.events
  (:require [taoensso.timbre :as log]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [bh.csrf :refer [?csrf-token]]))


(def default-header {:timeout         8000
                     :format          (ajax/transit-request-format)
                     :response-format (ajax/transit-response-format)
                     :headers         {"x-csrf-token" ?csrf-token}})


(re-frame/reg-event-db
  ::data-update
  (fn-traced [db [_ {:keys [id value]}]]
    ;(log/info "::data-update" id value)
    (assoc-in db [:sources id] value)))


(re-frame/reg-event-db
  ::init-locals
  (fn-traced [db [_ container init-vals]]
    ;(log/info "::init-locals" container init-vals)
    (if (get db container)
      (do
        ;(log/info "::init-locals // already exists")
        db)
      (do
        ;(log/info "::init-locals // adding")
        (assoc db container init-vals)))))


(re-frame/reg-event-db
  ::good-subscribe-result
  (fn-traced [db [_ source result]]
    (let [current (:subscribed db)]
      ;(log/info ":good-subscribe-result" source result
      ;  "////" current)
      (assoc db
        :subscribed (set (apply conj current source))
        :subscribe-error ""))))


(re-frame/reg-event-db
  ::bad-subscribe-result
  (fn-traced [db [_ source result]]
    (log/info "::bad-subscribe-result" source result)
    (assoc db
      :subscribe-error result)))


(re-frame/reg-event-fx
  ::subscribe-to
  (fn-traced [{:keys [db]} [_ source]]
    (let [user-id (:user-id db)]
      ;(log/info "::subscribe-to" source
      ; "////" {:user-id user-id :data-sources source}
      ; "////" ?csrf-token)
      {:http-xhrio (merge default-header
                     {:method     :post
                      :uri        "/subscribe/data-source"
                      :params     {:user-id user-id :data-sources source}
                      :on-success [::good-subscribe-result source]
                      :on-failure [::bad-subscribe-result source]})})))

