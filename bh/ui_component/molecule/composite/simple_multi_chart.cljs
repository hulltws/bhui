(ns bh.ui-component.molecule.composite.simple-multi-chart
  (:require [bh.ui-component.atom.chart.bar-chart :as chart]
            [bh.ui-component.utils.color :as color]
            [bh.ui-component.utils.helpers :as h]
            [bh.ui-component.utils.locals :as l]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.molecule.composite.simple-multi-chart")


(def sample-data chart/sample-data)


(def ui-definition
  {:components  {:ui/bar-chart   {:type :ui/component :name :rechart/bar
                                  :config-data []}
                 :ui/line-chart  {:type :ui/component :name :rechart/line
                                  :config-data []}
                 :topic/data     {:type :source/local :name :topic/data :default sample-data}}
   :links       {:topic/data     {:data {:ui/bar-chart   :data
                                         :ui/line-chart  :data}}}
   :grid-layout [{:i :ui/line-chart :x 0 :y 0 :w 10 :h 11 :static true}
                 {:i :ui/bar-chart :x 10 :y 0 :w 10 :h 11 :static true}]})


(def source-code '(let [def {:components  {:ui/bar-chart   {:type :ui/component :name :rechart/bar}
                                           :ui/line-chart  {:type :ui/component :name :rechart/line}
                                           :topic/data     {:type :source/local :name :topic/data :default sample-data}}
                             :links       {:topic/data     {:data {:ui/bar-chart   :data
                                                                   :ui/line-chart  :data}}}
                             :grid-layout [{:i :ui/line-chart :x 0 :y 0 :w 7 :h 11 :static true}
                                           {:i :ui/bar-chart :x 7 :y 0 :w 7 :h 11 :static true}]}]
                    [grid-widget/component
                     :data def
                     :component-id (h/path->keyword container-id "widget")]))

