(ns bh.data-source-handler
  (:require [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [bh.events :as events]))



(defmulti -data-source-msg-handler (fn [[evt & _]] evt))


(defn data-source-msg-handler
  [message]
  ;(log/info "data-source-msg-handler" ((fn [[evt & _]] evt) message) "////" (count message))
  (-data-source-msg-handler message))


(defmethod -data-source-msg-handler :default
  [message]
  (log/info "Unhandled -data-source-msg-handler event:" message))


(defmethod -data-source-msg-handler :some/broadcast
  [[_ content]]
  (rf/dispatch [::events/update-counter content]))


(defmethod -data-source-msg-handler :publish/data-update
  [[_ content]]
  ;(log/info "data-source-msg-handler :publish/data-update" content)
  (rf/dispatch [::events/data-update content]))
