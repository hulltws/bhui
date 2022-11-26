(ns bh.ui-component.atom.re-com.configure-toggle
  (:require [re-com.core :as rc]))



(defn configure-toggle
  "display a button that shows the 'settings' icon whn the user can clck
  to start editing/configuring things, and the 'floppy' icon when the user can save those changes

  ---

  - show? : (atom) should the button show the 'save' icon (true) or the 'settings' icon (false)
  - fn-to-apply : (fn) any function to call in addition to flipping the 'show?' atom back and forth
  "
  ([show? fn-to-apply]
   (let [make-editable-style {:md-icon-name "zmdi-settings"
                              :tooltip      "Configure this chart"}
         save-editable-style {:md-icon-name "zmdi-floppy"
                              :tooltip      "Save the configuration"}]
     ; TODO: can this be converted to (apply concat...)? (see https://clojuredesign.club/episode/080-apply-as-needed/)
     (reduce conj [rc/md-icon-button]
       (flatten
         (seq
           (merge {:class    "button"
                   :on-click #(do
                                (swap! show? not)
                                (when fn-to-apply (fn-to-apply)))}
             (if @show?
               save-editable-style make-editable-style)))))))

  ([show?]
   (configure-toggle show? nil)))
