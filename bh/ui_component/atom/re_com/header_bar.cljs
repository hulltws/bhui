(ns bh.ui-component.atom.re-com.header-bar
  (:require [re-com.core :as rc]))


(defn header-bar
  "creates a nice, simple header for the UI, displaying the app title, the server version number,
  the uuid returned by the server that is assigned to this user, and a button to log out of the
  server.

  > See also:
  >
  > [re-com](https://github.com/Day8/re-com)
  "
  [& {:keys [children]}]

  [rc/h-box :src (rc/at)
   :width "90%"
   :style {:background-color :lightgray
           :border           "solid"
           :border-width     "5px"
           :border-color     :blue
           :padding          "10px"}
   :justify :between
   :children children])