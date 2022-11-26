(ns bh.ui-component.utils.container
  (:require [bh.ui-component.utils.helpers :as h]
            [bh.ui-component.utils.locals :as l]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.utils.container")


(def default-composite {:blackboard {}})


(re-frame/reg-event-db
  :events/init-container
  (fn-traced [db [_ container]]
    ;(log/info ":events/init-container" container)
    (if (get-in db [:containers container])
      (do
        ;(log/info ":events/init-container // already exists")
        db)
      (do
        ;(log/info ":events/init-container // adding")
        (assoc-in db [:containers container] default-composite)))))


(defn init-container [container-id]
  (let [id         (h/path->keyword container-id)
        c          (h/path->keyword :containers container-id)
        blackboard (h/path->keyword container-id "blackboard")]

    ;(log/info "init-container" container-id id c blackboard)

    (re-frame/reg-sub
      c
      :<- [:containers]
      (fn [containers _]
        ;(log/info "sub" c id)
        (get containers id)))

    (re-frame/reg-sub
      blackboard
      :<- [c]
      (fn [w [_ path]]
        ;(log/info "blackboard sub" w blackboard)
        (get-in w path)))

    (re-frame/reg-event-db
      blackboard
      (fn [bb [_ id component-path new-val]]
        ;(log/info "container-event blackboard" id component-path new-val)
        (update-in bb [:containers id :blackboard]
          assoc component-path new-val)))))

;(re-frame/dispatch-sync [:events/init-container id])))


(defn subscribe-to-container [container-id [a & more :as component-path]]
  (let [p (l/compute-container-path container-id a more)]
    ;(log/info "subscribe-to-container" container-id component-path p)
    (re-frame/subscribe [p])))


(defn publish-to-container
  "
> NOTE: the re-frame event-handlers ***MUST*** be created beforehand, using [[init-container-locals]]

  ---

  - `container-id` : (string) name of the widget, typically a guid, but it can be any string you'd like
  - `component-path : (vector of keys [keywords or string]) the 'key' for the item that is being publised
  - `new-val` : (any) the new value to store at the given path

  `value-path` functions exactly like any other re-frame subscription, but relative to the
  `[:containers <component-id>]` in the overall `app-db`

  It is destructured as follows:

  | var        | type       | description                         |
  |:-----------|:----------:|:------------------------------------|
  | `a`        | keyword    | the (primary) value to subscribe to |
  | `& more`   | keyword(s) | any additional parts to the path    |

   ---

   #### EXAMPLES

  "
  [container-id component-path new-val]

  ;(log/info "publish-to-container-local" container-id component-path new-val)

  (let [p (h/path->keyword container-id "blackboard")]
    ;(log/info "publish-to-container" container-id component-path new-val p)
    (re-frame/dispatch [p component-path new-val])))


(defn build-container-subs
  "build the subscription needed to access all the container's configuration
  properties

  1. process-locals
  2. map over the result and call ui-utils/subscribe-to-container
  3. put the result into a hash-map
  "
  [container-id local-config]

  (->> (l/process-locals [] nil local-config)
    (map (fn [path]
           ;(log/info "build-container-subs" container-id path)
           {path (subscribe-to-container container-id path)}))
    (into {})))


(defn override-subs [override-source local-subs subs]
  ;(log/info "override-subs" override-source "//" local-subs "//" subs)
  (let [overrides (->> subs
                    (map (fn [path]
                           (let [s (get-in override-source path)]
                             ;(log/info "override-subs map" path "//" s)
                             (when (not (nil? s)) {path s})))))]

    ;(log/info "override-subs" override-source
    ;  "// (local-subs)" local-subs
    ;  "// (subs)" subs
    ;  "// (overrides)" overrides)

    (apply merge local-subs overrides)))



; revise override-subs
(comment
  (do
    (def data-source {:brush false
                      :uv    {:include true, :fill "#ff0000", :stackId ""}
                      :pv    {:include true, :fill "#00ff00", :stackId ""}
                      :tv    {:include true, :fill "#0000ff", :stackId ""}
                      :amt   {:include true, :fill "#f0f0f0", :stackId ""}})
    (def local-subs {[:tv :fill]     {:val "#82ca9d"},
                     [:uv :fill]     {:val "#8884d8"},
                     [:pv]           {:val {:include true, :fill "#ffc107", :stackId ""}},
                     [:pv :stackId]  {:val ""},
                     [:tv]           {:val {:include true, :fill "#82ca9d", :stackId ""}},
                     [:amt :stackId] {:val ""},
                     [:pv :include]  {:val true},
                     [:amt :fill]    {:val "#ff00ff"},
                     [:uv :include]  {:val true},
                     [:brush]        {:val nil},
                     [:tv :include]  {:val true}
                     [:amt]          {:val {:include true, :fill "#ff00ff", :stackId ""}}
                     [:pv :fill]     {:val "#ffc107"}
                     [:uv :stackId]  {:val ""}
                     [:tv :stackId]  {:val ""}
                     [:uv]           {:val {:include true, :fill "#8884d8", :stackId ""}}
                     [:amt :include] {:val true}})
    (def my-subs (l/process-locals [] nil data-source)))


  (->> my-subs
    (map (fn [path]
           ;(log/info "override-subs map" container-id path)
           (let [s (get-in data-source path)]
             (when s {path s}))))
    (apply merge local-subs))


  ())
