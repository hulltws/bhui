(ns bh.subscription-handlers
  (:require [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [bh.data-source-handler :as dh]))


(defmulti -event-msg-handler :id)


(defn event-msg-handler
  [{:as ev-msg :keys [id ?data event]}]
  ;(log/info "event-msg-handler" ?data "//" event)
  (-event-msg-handler ev-msg))


(defmethod -event-msg-handler :default
  [{:keys [event]}]
  (log/info "Unhandled event:" event))


(defmethod -event-msg-handler :chsk/recv
  [{:keys [_ ?data] :as msg}]

  ;(log/info "Push event from server:" ?data)
  (dh/data-source-msg-handler ?data))

