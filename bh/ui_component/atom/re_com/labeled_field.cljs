(ns bh.ui-component.atom.re-com.labeled-field
  (:require [taoensso.timbre :as log]
            [re-com.core :as rc]))


(defn labeled-field
  "creates a pair of UI elements: a label and some textual content, on a single horizontal line

  ---
   - label : (string) what to label this element
   - content : (any, converted to string) the actual value to show

  > See also:
  >
  > [re-com](https://github.com/Day8/re-com)
  "
  [label content]
  [rc/h-box
   :gap "5px"
   :children [[rc/title :style {:width "10%"}
               :level :level4 :label label]
              [rc/input-text :src (rc/at)
               :width "90%"
               :disabled? true
               :on-change #()
               :model (str content)]]])


(defn labeled-area
  "creates a pair of UI elements: a label and some textual content, on a horizontal line, but with the content
  presented in a (possibly multi-line) control that thte user can resize

  ---
  - label : (string) what to label this element
  - content : (any, converted to string) the actual value to show

  > See also:
  >
  > [re-com](https://github.com/Day8/re-com)
  "
  [label content & [rows]]
  [rc/h-box
   :gap "5px"
   :children [[rc/title :level :level4 :label label]
              [rc/input-textarea :src (rc/at)
               :rows (or rows 1)
               :disabled? true
               :on-change #()
               :model (str content)]]])

