(ns bh.ui-component.atom.worldwind.globe.react-support
  (:require ["worldwindjs" :as WorldWind]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.set :as set]
            [bh.ui-component.atom.worldwind.globe.projection :as proj]
            [bh.ui-component.atom.worldwind.globe.globe-time :as gt]
            [bh.ui-component.atom.worldwind.globe.layer :as l]
            [bh.ui-component.atom.worldwind.globe.layer.controls :as controls]
            [bh.ui-component.atom.worldwind.globe.layer.coordinates :as coords]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.atom.worldwind.globe.react-support")


(defn- find-in-children [children idx]
  (first (filter #(= (:id %) idx) children)))


(defn update-children [this new-children old-children]
  (let [new-keys (set (map :id new-children))
        old-keys (set (map :id old-children))
        added    (set/difference new-keys old-keys)
        removed  (set/difference old-keys new-keys)]

    ; remove old stuff
    (if removed
      (do
        ;(log/info "component-did-update removing" removed)
        (doall (map #(l/removeLayer this %) removed))
        (.redraw (.-wwd this))))

    ; add new stuff
    (if added
      (do
        ;(log/info "component-did-update adding" added)
        (doall
          (for [{:keys [id layer z]} (map #(find-in-children new-children %) added)]
            (do
              ;(log/info "adding" idx child)
              (l/addLayer this z layer))))
        (.redraw (.-wwd this))))))


(defn component-did-mount [dom-node state this]
  ;(log/info "component-did-mount" @state)

  (let [node (rdom/dom-node this)]
    ;; This will trigger a re-render of the component.
    (reset! dom-node node))

  (let [canvasId (.-id @dom-node)
        props    (r/props this)]

    ;(log/info "component-did-mount" (.-id @dom-node)
      ;"//// props" (r/props this)
    ;  "//// children" (r/children this))

    ;Create the WorldWindow using the ID of the canvas
    (set! (.-wwd this) (WorldWind/WorldWindow. canvasId))
    (swap! state assoc :wwd (.-wwd this))

    ; Apply projection support
    (set! (.-roundGlobe this) (.-globe (.-wwd this)))

    (if (:projection props)
      (do
        ;(log/info "set-projection"  canvasId(:projection props))
        (proj/change-projection this (:projection props))))

    (doall
      (for [{:keys [layer z]} (first (r/children this))]
        (do
          ;(log/info "adding layer" idx child)
          (l/addLayer this z layer))))

    ; add the controls layer
    (if (= :max (:min-max props))
      (l/addLayer this -1 (controls/controls this (str canvasId " Controls"))))

    ; add the coordinates layer
    (if (= :max (:min-max props))
      (l/addLayer this -1 (coords/coordinates this (str canvasId " Coordinates"))))

    (if (:time props)
      (do
        ;(log/info "set-time" canvasId (:time props))
        (gt/change-time this (:id @state) (:time props))))

    ;(log/info "component-did-mount" (sort (map #(.-displayName %) (.-layers (.-wwd this)))))

    (.redraw (.-wwd this))))


(defn component-did-update [dom-node state this old-argv]
  (let [[_ new-props new-children] (r/argv this)
        [old-id old-props old-children] old-argv]

    ;(log/info "component-did-update"
     ; "//// old-children" (sort (keys old-children))
     ; "//// old-props" old-props
     ; "//// new-children" (sort (keys new-children))
     ; "//// new-props" new-props)

    ;(log/info "projection"
    ;  (:projection old-props)
    ;  (:projection new-props))

    (if (not= (:projection old-props) (:projection new-props))
      (proj/change-projection this (:projection new-props)))

    (if (not= (:time old-props) (:time new-props))
      (do
        ;(log/info "update-time" (:id @state) (:time new-props))
        (gt/change-time this (:id @state) (:time new-props))))

    (update-children this new-children old-children)

    ;(log/info "component-did-update" (sort (map #(.-displayName %) (.-layers (.-wwd this)))))

    (.redraw (.-wwd this))))



(comment
  (def children [{:id ":worldwind-globe-demo.ww-globe Blue Marble", :layer "dummy", :z -1}
                 {:id ":worldwind-globe-demo.ww-globe Night", :layer "dummy", :z -1}
                 {:id ":worldwind-globe-demo.ww-globe Compass", :layer "dummy", :z -1}
                 {:id ":worldwind-globe-demo.ww-globe Star Field", :layer "dummy", :z -1}
                 {:id "image-15", :layer "dummy", :z 10}
                 {:id "image", :layer "dummy", :z 10}
                 {:id "image2", :layer "dummy", :z 10}
                 {:id "line1", :layer "dummy", :z 5}
                 {:id "line3", :layer "dummy", :z 5}
                 {:id "circle", :layer "dummy", :z 5}
                 {:id "circle2", :layer "dummy", :z 5}
                 {:id "line2", :layer "dummy", :z 5}
                 {:id "orlando", :layer "dummy", :z 10}
                 {:id "square", :layer "dummy", :z 5}
                 {:id "5-sided", :layer "dummy", :z 5}])

  (for [{:keys [layer z]} children]
    {layer z})




  ())

