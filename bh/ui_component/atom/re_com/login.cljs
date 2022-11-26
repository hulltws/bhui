(ns bh.ui-component.atom.re-com.login
  (:require [taoensso.timbre :as log]
            [reagent.core :as r]
            [re-com.core :as rc]
            [re-frame.core :as re-frame]

            [bh.ui-component.atom.re-com.button :as button]))


(defn login
  "returns a simple 'login' page (id/password). Self-contained.

  > See also:
  >
  > [re-com](https://github.com/Day8/re-com)
  "
  []
  (let [user-id (r/atom "")
        password (r/atom "")]
    [rc/v-box
     :src (rc/at)
     :margin "10px" :width "400px" :padding "10px"
     :style {:border "solid 1px" :border-radius "5px"}
     :children [[rc/h-box :src (rc/at)
                 :gap "10px"
                 :align :center
                 :justify :between
                 :children [[:h4 "User"]
                            [rc/input-text :src (rc/at)
                             :placeholder "user name"
                             :on-change #(reset! user-id %)
                             :model user-id]]]
                [rc/h-box :src (rc/at)
                 :gap "10px"
                 :align :center
                 :justify :between
                 :children [[:h4 "Password"]
                            [rc/input-password :src (rc/at)
                             :placeholder "password"
                             :on-change #(reset! password %)
                             :model password]]]
                [rc/gap :size "20px"]
                [rc/h-box
                 :children [[button/button "Register"]
                            [rc/gap :size "10px"]
                            [button/button "Login"]]]]]))

