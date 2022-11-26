(ns bh.ui-component.molecule.two-d-three-d-globe
  (:require [bh.ui-component.utils :as ui-utils]
            [re-com.core :as rc]
            [reagent.core :as r]
            [taoensso.timbre :as log]))


(def sample-data
  (r/atom {}))


(def source-code '[:div "2D/3D GLobe"])


(defn local-config [data]
  {:two-d-three-d :2d})


(defn config [component-id data]
  (merge ui-utils/default-pub-sub
    (local-config data)))


(defn- three-d-view [data component-id container-id]
  [:div "3D view"])


(defn- two-d-view [data component-id container-id]
  [:div "2D view"])


(defn- component-panel [data component-id container-id]

  (let [subscriptions (ui-utils/build-subs component-id (local-config data))]

    (fn [data component-id container-id]
      [:div
       ;which view do you want? :2d? :3d?
       [:button.button.is-info.is-outlined {:style    {:float :right}
                                            :on-click #(if (= :2d (ui-utils/resolve-sub subscriptions [:two-d-three-d]))
                                                         (ui-utils/dispatch-local component-id [:two-d-three-d] :3d)
                                                         (ui-utils/dispatch-local component-id [:two-d-three-d] :2d))}
        (condp = (ui-utils/resolve-sub subscriptions [:two-d-three-d])
          :2d "to 3d"
          :3d "to 2d")]

       (condp = (ui-utils/resolve-sub subscriptions [:two-d-three-d])
         :3d [three-d-view data component-id container-id]
         :2d [two-d-view data component-id container-id])])))


(defn component
  ([& {:keys [data component-id container-id]}]

   ;(log/info "two-2-three-3-globe" @data)

   (let [id (r/atom nil)]

     (fn []
       (when (nil? @id)
         (reset! id component-id)
         (ui-utils/init-container-locals @id (config @id data))
         (ui-utils/dispatch-local @id [:container] container-id))

       ;(log/info "component" @id)

       [component-panel data @id container-id]))))


(comment
  (do (def component-id "2d-3d-globe")
      (def data sample-data)
      (def id (r/atom component-id))
      (def container-id "2d-3d-globe-demo/globe"))

  (def s (ui-utils/build-subs component-id (local-config data)))
  (def two-d<->three-d (ui-utils/resolve-sub s [:two-d-three-d]))

  ())
