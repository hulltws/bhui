(ns bh.ui-component.atom.astrouxds.classification-marking
  (:require ["@astrouxds/react" :refer [RuxClassificationMarking]]))

(defn classification-marking [& {:keys [level footer?]}]
  (let [level-prop (when level {:classification level})
        footer-prop (when footer? {:footer footer?})]
    [:> RuxClassificationMarking (merge  {} level-prop footer-prop)]))
