(ns bh.ui-component.atom.astrouxds.progress-bar
  (:require ["@astrouxds/react" :refer [RuxProgress]]))

(defn progress-bar [& {:keys [value max hide-label]}]
  [:div {:style {:width 400}}
   [:> RuxProgress {:value value :max max :hide-label hide-label}]])

