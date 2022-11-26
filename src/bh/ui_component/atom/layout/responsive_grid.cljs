(ns bh.ui-component.atom.layout.responsive-grid
  (:require [taoensso.timbre :as log]
            ["react-grid-layout" :refer (Responsive WidthProvider)]))


(log/info "bh.ui-component.atom.layout.responsive-grid")


(def ResponsiveGridLayout (WidthProvider. Responsive))


(defn grid
  "use [react-grid-layout](https://github.com/react-grid-layout/react-grid-layout) to organize a bunch of children in a draggable grid

  ---

  Parameters are keyword identified as follows:

  | keyword     | type     | description            |
  |:------------|:--------:|:-----------------------|
  | `:id`       | string   | uniquely identify this particular grid, in case you have multiples |
  | `:children` | vector   | vector of hiccup that define each child to be placed into the grid |
  | `:layout`   | atom     | atom of layout 'records' that track where each child is drawn in the grid |
  | `:layoutFn` | function | function to update the layout atom when children are resized or move |
  | `:cols`     | atom     | atom wrapping an integer, which specifies the number of 'grid columns' |

> See also
>
> [react-grid-layout](https://github.com/react-grid-layout/react-grid-layout)
>
> [re-com](https://github.com/Day8/re-com)
  "
  [& {:keys [id children layout layoutFn widthFn
             cols width rowHeight compactType
             draggableHandle draggableCancel
             isDraggable isResizable] :as args}]

  ;(log/info "grid" id children layout layoutFn)

  (let [l {:lg layout :md layout :sm layout}]
    (into [:> ResponsiveGridLayout {:className       "layout"
                                    :id              id
                                    :layouts         l
                                    :breakpoints     {:lg 1000 :md 800 :sm 500 :xs 480 :xxs 0}
                                    :cols            {:lg 20 :md 20 :sm 20 :xs 20 :xxs 20}
                                    :width           (or width 1200)
                                    :rowHeight       (or rowHeight 25)
                                    :onLayoutChange  (or layoutFn #())
                                    :onWidthChange   (or widthFn #())
                                    :isDraggable     (or isDraggable true)
                                    :isResizable     (or isResizable true)
                                    :draggableHandle (or draggableHandle ".grid-toolbar")
                                    :draggableCancel (or draggableCancel ".grid-content")
                                    :compactType     (or compactType :vertical)}]
      children)))