(ns bh.ui-component.utils.locals
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require [bh.ui-component.utils.helpers :as h]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]))


(log/info "bh.ui-component.utils.locals")


(declare process-locals)
(declare create-container-local-sub)
(declare create-local-path-sub)
(declare create-container-local-event)
(declare create-local-path-event)


(re-frame/reg-event-db
  :events/init-container-locals
  (fn-traced [db [_ container values]]
    ;(log/info "::init-container-locals" container init-vals)
    (if (= (get-in db [:containers container]) values)
      (do
        ;(log/info "::init-container-locals // already exists")
        db)
      (do
        ;(log/info "::init-container-locals // adding")
        (assoc-in db [:containers container] values)))))


(re-frame/reg-event-db
  :events/init-local-path
  (fn-traced [db [_ local-path values]]
    ;(log/info "::init-local-path" container init-vals)
    (if (= (get-in db local-path) values)
      (do
        ;(log/info "::init-local-path // already exists")
        db)
      (do
        ;(log/info "::init-local-path // adding")
        (assoc-in db local-path values)))))


(defn init-local-values
  "add the given 'tree' of values into the app-db under the `[:containers <container-id as a keyword>] path

  `component-id` is automatically converted into a `keyword`

  In cases where there are multiple containers of the same 'type', using 'locals' keeps each
  instance's local state away from all the others, so changing the state of one does _not_ change
  them all.

  ---

  - `component-id` : (string) id of the component, passed as a string so we can use generated values (like guids)
  - `values` : (hash-map) hash-map (tree) of values specific to _this_ container.

  "
  [component-id values]
  (let [target (keyword component-id)
        path   [:events/init-container-locals target values]]
    ;(log/info "init-local-values" path)
    (re-frame/dispatch-sync path)))


(defn update-local-values [component-id values]
  (let [target         (keyword component-id)
        old            (get-in @re-frame.db/app-db [:containers target])
        old-vals       (->> old
                         (process-locals [] nil)
                         (filter #(= 1 (count %)))
                         (into #{}))
        new-vals-paths (process-locals [] nil values)
        new-vals       (->> new-vals-paths
                         (filter #(= 1 (count %)))
                         (into #{}))
        diff           (clojure.set/difference new-vals old-vals)
        merged-values  (merge old
                         (->> diff
                           (map (fn [[path]]
                                  {path (get values path)}))
                           (into {})))]

    ;(log/info "update-local-values" diff "//" old "//" merged-values)

    (when (not (empty? diff))
      (re-frame/dispatch-sync
        [:events/init-container-locals target merged-values])

      (doall
        ; TODO: consider using locals-and-defaults to put the actual default into the subscription rather than 'nil'
        (map #(create-container-local-sub component-id % nil) new-vals-paths))

      (doall
        (map #(create-container-local-event component-id %) new-vals-paths)))))


(defn update-local-path-values [component-id target-path values]

  ;(log/info "update-local-path-values" component-id "//" values)

  ; TODO: can this be converted to (apply concat...)? (see https://clojuredesign.club/episode/080-apply-as-needed/)
  (let [data-path      (reduce conj [(h/path->keyword component-id)] target-path)
        container-path    (reduce conj [:containers (h/path->keyword component-id)] target-path)
        old            (get-in @re-frame.db/app-db container-path)
        old-vals       (->> old
                         (process-locals [] nil)
                         (filter #(= 1 (count %)))
                         (into #{}))
        new-vals-paths (process-locals [] nil values)
        new-vals       (->> new-vals-paths
                         (filter #(= 1 (count %)))
                         (into #{}))
        diff           (clojure.set/difference new-vals old-vals)
        merged-values  (merge old
                         (->> diff
                           (map (fn [[path]]
                                  {path (get values path)}))
                           (into {})))]

    ;(log/info "update-local-path-values (merged)" component-id "//" diff "//" old "//" merged-values)

    (when (not (empty? diff))
      ;(log/info "update-local-path-values (target)" container-path "//" data-path "//" new-vals-paths)

      (re-frame/dispatch-sync
        [:events/init-local-path container-path merged-values])

      (doall
        ; TODO: consider using locals-and-defaults to put the actual default into the subscription rather than 'nil'
        ; TODO: can this be converted to (apply concat...)? (see https://clojuredesign.club/episode/080-apply-as-needed/)
        (map #(create-local-path-sub (reduce conj data-path %) nil) new-vals-paths))

      (doall
        ; TODO: can this be converted to (apply concat...)? (see https://clojuredesign.club/episode/080-apply-as-needed/)
        (map #(create-local-path-event (reduce conj data-path %)) new-vals-paths)))

    merged-values))


(defn process-branch [accum root k v]
  (do
    ;(println "branch" v [root k] accum)
    (as-> accum x
      (conj x (if root
                (if (vector? root)
                  (conj root k)
                  [root k])
                [k]))
      (apply conj x (process-locals []
                      (if root
                        (if (vector? root)
                          (conj root k)
                          [root k])
                        k)
                      v)))))


(defn process-leaf [accum root k]
  (do
    ;(println "leaf" root k accum)
    (conj accum (if root
                  (if (vector? root)
                    (conj root k)
                    [root k])
                  [k]))))


(defn process-locals
  "recursively walks through the 'tree' of values and computes the 'path vector' to reach each
  value.

  For example:

  `{:value-1 \"dummy\" :value-2 {:nested-value \"dummy\"}}`

  would have paths:

  `[[:value-1] [:value-2] [:value-2 :nested-value]]`

  This is a preparation step for creating and registering the re-frame
  [subscription handlers](https://day8.github.io/re-frame/subscriptions/), so we must
  create a vector for each value in the 'tree' so other code, like a UI 'container', can subscribe to
  the value and automatically 'update and render'.

  ---

  - a : (vector) the starting value to accumulate the result into, typically `[]`
  - r : (any) the initial value of the 'root' item, typically `()`
  - t : (hash-map) the 'tree' of values to process

  Returns a `vector` of `vector`s of `keyword`s, where each is the path (relative to the initial `r`^*^) to
  the specific value of interest.

> Note: ^*^ typically we sort out the 'base' for the relative paths separately, using
> [[create-container-local-sub]]

  "
  [a r t]
  ;(println "process-locals" a r t)
  (loop [accum a root r tree t]
    ;(println "process" tree root accum)
    (if (empty? tree)
      (do
        ;(println "result" accum)
        accum)
      (let [[k v] (first tree)]
        ;(println "let" k v)
        (recur (if (map? v)
                 (process-branch accum root k v)
                 (process-leaf accum root k))
          root
          (rest tree))))))


(defn compute-container-path [container-id a more]
  (h/path->keyword container-id "blackboard" a more))


(defn compute-container-deps [container-id a more]
  (if more
    (h/path->keyword container-id a (drop-last more))
    (h/path->keyword container-id)))


(defn compute-deps [a more]
  (if more
    (h/path->keyword a (drop-last more))
    (h/path->keyword a)))


(defn create-container-sub
  "create and registers a re-frame [subscription handler](https://day8.github.io/re-frame/subscriptions/)
  for the `container-id` (as a keyword) inside the `:containers` top-level key in the `app-db`.

  ---

  - `container-id` : (string) id for the container, using a string means we can use generated values, like a guid, for the id

  "
  [container-id]
  (let [id (h/path->keyword container-id)]

    ;(log/info "create-container-sub" id)

    (re-frame/reg-sub
      id
      :<- [:containers]
      (fn [containers _]
        ;(log/info "sub" w id)
        (get containers id)))))


(defn create-container-local-sub
  "create and registers a re-frame [subscription handler](https://day8.github.io/re-frame/subscriptions/)
  for the value at the path inside the [`:containers` `container-id as a keyword`] key in the `app-db`.

  ---

  - `container-id` : (string) id for the container, using a string means we can use generated values, like a guid, for the id
  - `value-path` : (vector of keywords) the path into the container's values to locate the specific one for this subscription

  `value-path` functions exactly like any other re-frame subscription, but relative to the
  `[:containers <container-id as a keyword>]` in the overall `app-db`

  It is destructured as follows:

  | var        | type       | description                         |
  |:-----------|:----------:|:------------------------------------|
  | `a`        | keyword    | the (primary) value to subscribe to |
  | `& more`   | keyword(s) | any additional parts to the path    |

  We use 'cascading subscriptions', i.e., [`Layer-3` subscriptions](https://day8.github.io/re-frame/subscriptions/#reg-sub),
  to organize things. In order to generate unique ids for each subscription, we concatenate the
  path into a single value:

  assuming: `(def container-id \"some-guid\")` then path `[:value-2 :nested-value]` would be converted into the subscription named
  `:some-guid/value-2.nested-value`

> Note: so developer don't need to understand or even remember this encoding scheme, use the [[subscribe-local]] helper function
> in place of standard re-frame subscription calls. It provides the same result, and does all the encoding for you.
  "
  [container-id [a & more :as value-path] default]
  (let [p    (h/path->keyword container-id a more)
        dep  (compute-container-deps container-id a more)
        item (h/path->keyword (if more (last more) a))]

    ;(log/info "create-container-local-sub" p
    ;  ":<-" dep
    ;  "item" item)

    (re-frame/reg-sub
      p
      :<- [dep]
      (fn [container _]
        ;(log/info "sub" p dep container (last more))
        (or (get container item) default)))))


(defn create-local-path-sub [[a & more :as value-path] default]
  (let [p    (h/path->keyword a more)
        dep  (compute-deps a more)
        item (h/path->keyword (if more (last more) a))]

    ;(log/info "create-local-path-sub"
    ;  value-path
    ;  "//" p
    ;  ":<-" dep
    ;  "//" item)

    (re-frame/reg-sub
      p
      :<- [dep]
      (fn [container _]
        ;(log/info "sub" p dep container (last more))
        (or (get container item) default)))))


(defn create-container-event
  "create and registers a re-frame [event handler](https://day8.github.io/re-frame/dominoes-30k/#domino-2-event-handling)
  for the `container-id` (as a keyword) inside the `:containers` top-level key in the `app-db`.

  ---

  - `container-id` : (string) id for the widget, using a string means we can use generated values, like a guid, for the id

  "
  [container-id]
  (let [id (h/path->keyword container-id)]

    ;(log/info "create-container-event" id)

    (re-frame/reg-event-db
      id
      (fn [db [_ new-val]]
        ;(log/info "event" w id)
        (assoc-in db [:containers id] new-val)))))


(defn create-container-local-event
  "create and registers a re-frame [event handler](https://day8.github.io/re-frame/dominoes-30k/#domino-2-event-handling)
  for the value at the path inside the [`:containers` `container-id as a keyword`] key in the `app-db`.

  ---

  - `container-id` : (string) id for the container, using a string means we can use generated values, like a guid, for the id
  - `value-path` : (vector of keywords) the path into the widget values to locate the specific one for this subscription

  `value-path` functions exactly like any other re-frame subscription, but relative to the
  `[:containers <container-id as a keyword>]` in the overall `app-db`

  It is destructured as follows:

  | var        | type       | description                         |
  |:-----------|:----------:|:------------------------------------|
  | `a`        | keyword    | the (primary) value to subscribe to |
  | `& more`   | keyword(s) | any additional parts to the path    |

  We use 'cascading subscriptions', i.e., [`Layer-3` subscriptions](https://day8.github.io/re-frame/subscriptions/#reg-sub),
  to organize things. In order to generate unique ids for each subscription, we concatenate the
  path into a single value:

  assuming: `(def widget-wid \"some-guid\")` then path `[:value-2 :nested-value]` would be converted into the subscription named
  `:some-guid/value-2.nested-value`

> Note: so developer don't need to understand or even remember this encoding scheme, use the [[subscribe-local]] helper function
> in place of standard re-frame subscription calls. It provides the same result, and does all the encoding for you.
  "
  [container-id [a & more :as value-path]]
  (let [p (h/path->keyword container-id a more)]

    ;(log/info "create-container-local-event" p
    ;  "apply conj" (apply conj [:containers (h/path->keyword container-id)] (map h/path->keyword value-path)))

    (re-frame/reg-event-db
      p
      (fn [db [_ new-val]]
        ;(log/info "event" p new-val)

        ; NOTE: this "default" processing could be overridden (using an optional keyword)
        ; to perform more custom functions (like incremental updates to a collection)
        ;
        (assoc-in db
          (apply conj [:containers (h/path->keyword container-id)] (map h/path->keyword value-path))
          new-val)))))


(defn create-local-path-event [value-path]
  (let [p (h/path->keyword value-path)]                     ;a more)]

    ;(log/info "create-local-path-event"
    ;  value-path
    ;  "//" p
    ;  "//" (reduce conj [:containers] (map h/path->keyword value-path)))

    (re-frame/reg-event-db
      p
      (fn [db [_ new-val]]
        ;(log/info "event" p new-val)

        ; NOTE: this "default" processing could be overridden (using an optional keyword)
        ; to perform more custom functions (like incremental updates to a collection)
        ;
        (assoc-in db
          ; TODO: can this be converted to (apply concat...)? (see https://clojuredesign.club/episode/080-apply-as-needed/)
          (reduce conj [:containers] (map h/path->keyword value-path))
          new-val)))))


(defn init-container-locals
  "1. adds the `locals-and-defaults` into the `app-db` in the correct location
  2. creates and registers a subscription to `:containers/<container-id>`
  3. creates and registers a subscription (cascaded off `:containers/<container-id>`) for each relative path in `locals-and-defaults`
  4. creates and registers an event handler for`:containers/<container-id>`
  5. creates and registers an event handler for each relative path in `locals-and-defaults`

  `locals-and-defaults` provides both the structure used to create the subscriptions and the default values when a new widget is
  created

  ---

  - `container-id` : (string) id for the container, using a string means we can use generated values, like a guid, for the id
  - `locals-and-defaults` : (hash-map) hash-map (tree) of values specific to _this_ widget.

> TODO: need to build the reg-event-db handlers so users/ui can change the locals
  "
  [container-id locals-and-defaults]
  (let [paths (process-locals [] nil locals-and-defaults)]

    ;(log/info "init-container-locals" container-id
    ;  "//" paths
    ;  "//" locals-and-defaults)

    ; load the app-db with the default values
    (init-local-values container-id locals-and-defaults)

    ; create subscriptions
    (create-container-sub container-id)
    (doall
      ; TODO: consider using locals-and-defaults to put the actual default into the subscription rather than 'nil'
      (map #(create-container-local-sub container-id % nil) paths))

    ; create event handlers
    (create-container-event container-id)
    (doall
      (map #(create-container-local-event container-id %) paths))))


(defn subscribe-local
  "constructs a Re-frame subscription to a local value since the given
  widget's 'locals' in the `app-db`. This way the developer isn't concerned about the
  exact location of the data in the `app-db`.

  The container-id string will be converted into a keyword as appropriate to access the
  registered subscription, so you can freely use generated values as widget identifiers

> NOTE: the re-frame subscriptions ***MUST*** be created beforehand, using [[init-container-locals]]

  ---

  - `container-id` : (string) name of the widget, typically a guid, but it can be any string you'd like
  - `value-path : (vector of keywords) the path into the widget values to locate the specific one for this subscription

  `value-path` functions exactly like any other re-frame subscription, but relative to the
  `[:containers <container-id>]` in the overall `app-db`

  It is destructured as follows:

  | var        | type       | description                         |
  |:-----------|:----------:|:------------------------------------|
  | `a`        | keyword    | the (primary) value to subscribe to |
  | `& more`   | keyword(s) | any additional parts to the path    |

  Returns a `reagent/reaction` which can be used exactly like any other re-frame subscription

   ---

   #### EXAMPLES

   Assume

   `(def container-id \"some-guid\")`

   and

   `(def some-guid-locals {:value-1 \"default\" :value-2 {:nested-value \"default\"}})`

   | desired subscription | call                                         |
   |:--------------------:| :--------------------------------------------|
   | `:value-1`           | `(subscribe-local \"some-guid\" [:value-1])` |
   | `:value-2`           | `(subscribe-local \"some-guid\" [:value-2])` |
   | `:nested-value`      | `(subscribe-local \"some-guid\" [:value-2 :nested-value])` |
  "
  [container-id [a & more :as value-path]]
  (let [p (h/path->keyword container-id a more)]
    ;(log/info "subscribe-local" container-id value-path p)
    (re-frame/subscribe [p])))


(defn resolve-subscribe-local [container-id [a & more :as value-path]]
  (let [p         (h/path->keyword container-id a more)
        sub-ok    (re-frame/subscribe [p])
        get-ok    (let [x (re-frame/subscribe [(h/path->keyword container-id a)])]
                    (if x (get @x more) nil))
        get-in-ok (let [x (re-frame/subscribe [(h/path->keyword container-id)])]
                    (if x (get-in @x value-path)))]

    (log/info "resolve-subscribe-local (ret)" container-id "," value-path "//" sub-ok "//" get-ok "//" get-in-ok)

    (if sub-ok
      sub-ok
      (if get-ok
        (reaction get-ok)
        (if get-in-ok
          (reaction get-in-ok)
          nil)))))


(defn dispatch-local
  "constructs a Re-frame event dispatch call to a local value stored in the given
  widget's 'locals' in the `app-db`. This way the developer isn't concerned about the
  exact location of the data in the `app-db`.

  The container-id string will be converted into a keyword as appropriate to access the
  registered subscription, so you can freely use generated values as widget identifiers

> NOTE: the re-frame event-handlers ***MUST*** be created beforehand, using [[init-container-locals]]

  ---

  - `container-id` : (string) name of the container, typically a guid, but it can be any string you'd like
  - `value-path : (vector of keywords) the path into the widget values to locate the specific one for this subscription
  - `new-val` : (any) the new value to store at the given path

  `value-path` functions exactly like any other re-frame subscription, but relative to the
  `[:containers <container-id>]` in the overall `app-db`

  It is destructured as follows:

  | var        | type       | description                         |
  |:-----------|:----------:|:------------------------------------|
  | `a`        | keyword    | the (primary) value to subscribe to |
  | `& more`   | keyword(s) | any additional parts to the path    |

   ---

   #### EXAMPLES

  "
  [container-id [a & more :as value-path] new-val]

  ;(log/info "dispatch-local" container-id value-path new-val)

  (let [p (h/path->keyword container-id a more)]
    ;(log/info "dispatch-local" container-id "//" value-path "//" p "//" new-val)
    (re-frame/dispatch [p new-val])))


(defn apply-local
  "applies the given function (fn-to-apply) to the value found in the app-db and then
  dispatches that new value to replace the old value using dispatch-local with the
  original value-path vector

  ---

  - fn-to-apply : (function) takes 1 parameter, the original value at the scubscription vector
  "
  [container-id [a & more :as value-path] fn-to-apply]

  (let [p          (h/path->keyword container-id a more)
        orig-value @(re-frame/subscribe [p])
        new-value  (fn-to-apply orig-value)]
    ;(log/info "apply-local"
    ;  "//" p
    ;  "//" orig-value
    ;  "//" new-value)
    (dispatch-local container-id [value-path] new-value)))


(defn build-subs
  "build the subscription needed to access all the 'local' configuration
  properties

  1. process-locals
  2. map over the result and call ui-utils/subscribe-local
  3. put the result into a hash-map
  "
  [component-id local-config]

  ;(log/info "build-subs" component-id "//" local-config)

  (let [ret (->> (process-locals [] nil local-config)
              (map (fn [path]
                     {path (subscribe-local component-id path)}))
              (into {}))]

    ;(log/info "build-subs" component-id "//" ret)
    ret))


(defn resolve-sub [subs path]
  (let [ret (get subs (->> path
                        (map h/path->keyword)
                        (into [])))]
    ;(log/info "resolve-sub" subs "// (path)" path "// (ret)" ret)
    (if (or (instance? reagent.ratom.RAtom ret)
          (instance? Atom ret)
          (instance? reagent.ratom.Reaction ret))
      @ret
      ret)))


(comment
  (re-frame/subscribe [:chart-remote-data-demo.widget])
  (re-frame/subscribe [:chart-remote-data-demo.widget.ui.bar-chart])
  (re-frame/subscribe [:chart-remote-data-demo.widget.ui.bar-chart.x-axis])
  (re-frame/subscribe [:chart-remote-data-demo.widget.ui.bar-chart.x-axis.include])


  (do
    (def container-id [:simple-multi-chart.widget :blackboard :topic.data])
    (def a :data)
    (def more nil)
    (def value-path [:data]))

  (def sub-ok (re-frame/subscribe (h/path->keyword container-id [:data])))
  (def get-ok (let [x (re-frame/subscribe [(h/path->keyword container-id a)])]
                (if x (get @x more) nil)))
  (def get-in-ok (let [x (re-frame/subscribe [(h/path->keyword container-id)])]
                   (if x (get-in @x value-path))))


  ())



(comment
  (def db @re-frame.db/app-db)
  (def container :bar-chart-2-demo.bar-chart-2)
  (def init-vals {:y-axis {:include true, :dataKey "", :orientation :left, :scale "auto"},
                  :grid   {:include true, :strokeDasharray {:dash "3", :space "3"}, :stroke "#a9a9a9"}})
  (def init-vals {:amt    {:include true :fill "#ffffff" :stackId ""}
                  :tv     {:include true :fill "#ffffff" :stackId ""}
                  :y-axis {:include true, :dataKey "", :orientation :left, :scale "auto"},
                  :grid   {:include true, :strokeDasharray {:dash "3", :space "3"}, :stroke "#a9a9a9"},})
  (def init-vals {:y-axis {:include false, :dataKey "", :orientation :left, :scale "auto"},
                  :grid   {:include true, :strokeDasharray {:dash "3", :space "3"}, :stroke "#a9a9a9"},})

  (def old-vals (get-in db [:containers container]))
  (def old-vals-flat (->> old-vals
                       (process-locals [] nil)
                       (filter #(= 1 (count %)))
                       (into #{})))
  (def init-vals-flat (->> init-vals
                        (process-locals [] nil)
                        (filter #(= 1 (count %)))
                        (into #{})))
  (def diff (clojure.set/difference init-vals-flat old-vals-flat))


  (let [db             @re-frame.db/app-db
        container      :bar-chart-2-demo.bar-chart-2
        init-vals      {:amt    {:include true :fill "#ffffff" :stackId ""}
                        :tv     {:include true :fill "#ff00ff" :stackId ""}
                        :y-axis {:include true :dataKey "" :orientation :left :scale "auto"}
                        :grid   {:include true :strokeDasharray {:dash "3" :space "3"}
                                 :stroke  "#a9a9a9"}}
        old-vals       (get-in db [:containers container])
        old-vals-flat  (->> old-vals
                         (process-locals [] nil)
                         (filter #(= 1 (count %)))
                         (into #{}))
        init-vals-flat (->> init-vals
                         (process-locals [] nil)
                         (filter #(= 1 (count %)))
                         (into #{}))
        diff           (clojure.set/difference init-vals-flat old-vals-flat)]

    (merge old-vals
      (->> diff
        (map (fn [[path]]
               {path (get init-vals path)}))
        (into {}))))

  ())


; play with dispatch-local and related things
(comment
  (def container-id nil)
  (def path [:uv :fill])
  (def path [:blackboard :topic/config :uv :fill])
  (def path [:container-widget :blackboard :topic/config :uv :fill])


  (let [[a & more] path]
    (h/path->keyword container-id a more))





  ())


