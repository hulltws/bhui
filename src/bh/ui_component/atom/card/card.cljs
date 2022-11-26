(ns bh.ui-component.atom.card.card
  (:require [re-frame.core :as rf]))



(def source-code '[:div.card {:style (or style default-card-style)}
                   [:div.card-header
                    [:div.card-header-title title]]
                   [:div.card-content
                    (or content [:div#empty])]])

(def default-background "#9CA8B3")
(def default-color "#FF")
(def default-card-style {:width      "100%"
                         :height     "100%"
                         :margin     :auto
                         :overflow   "hidden"
                         :background default-background
                         :color      default-color})


(defn card [& {:keys [style header-style title content]}]
  [:div.card {:style (or style default-card-style)}
   [:div.card-header
    [:div.card-header-title title]]

   [:div.card-content
    (or content [:div#empty])]])


(def meta-data {:bh/card {:component card
                          :ports     {:title   :port/sink
                                      :content :port/sink}}})


(rf/dispatch-sync [:register-meta meta-data])




