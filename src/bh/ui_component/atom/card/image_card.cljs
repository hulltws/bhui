(ns bh.ui-component.atom.card.image-card
  (:require [re-com.core :as rc]))


(def source-code '[:div.card {:style (or style default-card-style)}
                   [:div.card-image
                    [:figure.image {:style (or image-style default-image-style)}
                     [:img {:src (or image default-image) :alt (or alt-text title)}]]]
                   [:div.card-content
                    [:p.title.is-4 title]
                    content]])


(def default-background "#9CA8B3")
(def default-color "#FF")
(def default-card-style {:width           "100%" :height "100%"
                         :overflow        "hidden"
                         :background      default-background
                         :color           default-color
                         :display         :flex
                         :flex-direction  :column
                         :justify-content :center
                         :align-items     :center})
(def default-image-style {:width        "75px" :height "75px"
                          :display      :block
                          :margin-left  :auto
                          :margin-right :auto})
(def default-image "imgs/hammer-icon-16x16.png")


(defn card [& {:keys [style title image image-style alt-text content]}]
  [:div.card {:style (or style default-card-style)}
   [:div.card-image
    [rc/v-box
     :children [[rc/gap :size "10px"]
                [:figure.image {:style (or image-style default-image-style)}
                 [:img {:src (or image default-image) :alt (or alt-text title)}]]]]]
   [:div.card-content {:style {:text-align :center}}
    [:p.title.is-4 title]
    content]])



