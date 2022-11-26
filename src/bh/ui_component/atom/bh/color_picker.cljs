(ns bh.ui-component.atom.bh.color-picker
  (:require [bh.ui-component.utils.helpers :as h]
            [reagent.core :as r]
            [re-com.core :as rc]
            ["react-colorful" :refer [HexColorPicker RgbaColorPicker]]
            [taoensso.timbre :as log]))


(defn rgba-color-picker [& {:keys [color on-change]}]
  ;(log/info "rgba-color-picker" color)
  [:> RgbaColorPicker {:color     color
                       :on-change #(on-change %)}])


(defn hex-color-picker [& {:keys [color on-change]}]
  ;(log/info "hex-color-picker" color)
  [:> HexColorPicker {:color     color
                      :on-change #(on-change %)}])


