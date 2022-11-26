(ns bh.ui-component.molecule.composite.chart-remote-data)


(def ui-definition
  {:components   {:topic/measurements {:type :source/remote :name :source/measurements}
                  :ui/bar-chart       {:type :ui/component :name :rechart/bar}}

   :links        {:topic/measurements {:data {:ui/bar-chart :data}}}

   :grid-layout  [{:i :ui/bar-chart :x 0 :y 0 :w 20 :h 11 :static true}]})


(def source-code '(let [def {:title        "Chart with remote Data"
                             :component-id :chart-remote-data
                             :components   {:ui/bar-chart       {:type :ui/component :name :rechart/bar}
                                            :topic/measurements {:type :source/remote :name :source/measurements}}
                             :links        {:topic/measurements {:data {:ui/bar-chart :data}}}
                             :grid-layout  [{:i :ui/bar-chart :x 0 :y 0 :w 20 :h 11 :static true}]}]
                    [grid-widget/component
                     :data def
                     :component-id (h/path->keyword container-id "widget")]))
