(ns bh.ui-component.utils.color)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; Color Support
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


; see colors at https://htmlcolors.com
(def default-stroke-fill-colors ["#8884d8" "#ffc107" "#82ca9d"
                                 "#ff00ff" "#00e5ff" "#4db6ac"
                                 "#83a6ed" "#8dd1e1" "#a4de6c"
                                 "#ffff00" "#ff0000" "#00ff00"
                                 "#0000ff" "#009999" "#d7e62b"])


(defn get-color [idx]
  (let [i (mod idx (count default-stroke-fill-colors))]
    (get default-stroke-fill-colors i)))


(defn hex->rgba
  "convert a color in hexadecimal (string) into a hash-map of RGBA

  ---

  - hex-color : (string) hex encoded color, such as \"#ff0000\" (red) or \"#00CED1\" (dark turquoise)

  returns hash-map containing:

  | key  | type    | range   | description    |
  |:-----|:-------:|:-------:|:---------------|
  | `:r` | integer | 0-255   | red value      |
  | `:g` | integer | 0-255   | green value    |
  | `:b` | integer | 0-255   | blue value     |
  | `:a` | float   | 1.0     | alpha channel value, always returned as 1.0 |
  "
  [hex-color]

  (let [stripped (apply str (rest hex-color))
        [r g b] (re-seq #"\w\w" stripped)]
    {:r (js/parseInt r 16)
     :g (js/parseInt g 16)
     :b (js/parseInt b 16)
     :a 1.0}))


(defn rgba-normal
  "convert a color hash-map of RGBA to a hash-map where the values are
  in the range of 0.0 - 1.0

  ---

  - hex-color : (string) hex encoded color, such as \"#ff0000\" (red) or \"#00CED1\" (dark turquoise)

  returns hash-map containing:

  | key  | type    | range   | description    |
  |:-----|:-------:|:-------:|:---------------|
  | `:r` | float   | 0.0-1.0 | red value      |
  | `:g` | float   | 0.0-1.0 | green value    |
  | `:b` | float   | 0.0-1.0 | blue value     |
  | `:a` | float   | 1.0     | alpha channel value, always returned as 1.0 |
  "
  [{:keys [r g b a]}]

  {:r (/ r 255) :g (/ g 255) :b (/ b 255) :a a})


(defn normal-map->rgba
  "convert a color hash-map of RGBA (0-1.0) to a hash-map where the values are
  in the range of 0 255

  ---

  - color : (string) float encoded color

  returns hash-map containing:

  | key  | type    | range   | description    |
  |:-----|:-------:|:-------:|:---------------|
  | `:r` | float   | 0.0-1.0 | red value      |
  | `:g` | float   | 0.0-1.0 | green value    |
  | `:b` | float   | 0.0-1.0 | blue value     |
  | `:a` | float   | 1.0     | alpha channel value, always returned as 1.0 |
  "
  [{:keys [r g b a]}]

  {:r (* r 255) :g (* g 255) :b (* b 255) :a a})


(defn normal->rgba
  ([r g b a]
   {:r (.round js/Math (* r 255)) :g (.round js/Math (* g 255))
    :b (.round js/Math (* b 255)) :a (.round js/Math (* a 255))})

  ([[r g b a]]
   {:r (.round js/Math (* r 255)) :g (.round js/Math (* g 255))
    :b (.round js/Math (* b 255)) :a (.round js/Math (* a 255))}))


(defn rgba-map->rgba-vector [{:keys [r g b a] :as rgba-map}]
  [r g b a])


(defn rgba-map->js-function [{:keys [r g b a] :as rgba-map}]
  (str "rgba(" r ", " g ", " b ", " a ")"))


(defn rgba->hex
  "convert a color hash-map of RGBA into a hexadecimal (string)

  ---

  - rgba-color : (hash-map) containing:

  | key  | type    | range   | description    |
  |:-----|:-------:|:-------:|:---------------|
  | `:r` | integer | 0-255   | red value      |
  | `:g` | integer | 0-255   | green value    |
  | `:b` | integer | 0-255   | blue value     |
  | `:a` | float   | 1.0     | alpha channel value, always returned as 1.0 |

  returns string containing the encoded color, such as \"#ff0000\" (red) or \"#00CED1\" (dark turquoise)
  "
  ([{:keys [r g b] :as rgba-color}]

   (let [convertFn (fn [x]
                     (let [s (.toString (js/Number. x) 16)]
                       (if (= (count s) 1)
                         (str "0" s)
                         s)))]
     (str "#" (convertFn r) (convertFn g) (convertFn b))))

  ([r g b _]
   (let [convertFn (fn [x]
                     (let [s (.toString (js/Number. x) 16)]
                       (if (= (count s) 1)
                         (str "0" s)
                         s)))]
     (str "#" (convertFn r) (convertFn g) (convertFn b)))))


(defn hash->rgba
  "converts a color represented as a ClojureScript hash-map into a format compatible with
  Javascript, HTML, and CSS.

  ---

  - hash-color : (hash-map) containing:

  | key  | type    | range   | description    |
  |:-----|:-------:|:-------:|:---------------|
  | `:r` | integer | 0-255   | red value      |
  | `:g` | integer | 0-255   | green value    |
  | `:b` | integer | 0-255   | blue value     |
  | `:a` | float   | 0-1.0   | alpha channel  |

  returns a Javascript command (string) that various HMTL `:style`s will treat as an rgba color
  "
  [{:keys [r g b a] :as hash-color}]

  (str "rgba(" r "," g "," b "," a ")"))


(defn match-colors-hex [hex-color]
  (let [rgba (hex->rgba hex-color)]
    [:custom
     (rgba-map->js-function rgba)
     (rgba-map->rgba-vector rgba)
     (-> hex-color hex->rgba rgba-normal rgba-map->rgba-vector)
     hex-color]))


(defn match-colors-rgba [rgba-color]
  (let [hex (rgba->hex rgba-color)]
    [:custom
     (rgba-map->js-function rgba-color)
     (rgba-map->rgba-vector rgba-color)
     (rgba-map->rgba-vector (rgba-normal rgba-color))
     (rgba->hex rgba-color)]))


(defn relative-luminance
  "computes _relative luminance_ per the [W3C](https://www.w3.org/TR/WCAG20/#relativeluminancedef)

  typically, this value is uses to determine the proper color (`:white` or `:black`) to use with a
  colored background.

  ---
  - color : (hash-map) containing:

  | key  | type    | range   | description    |
  |:-----|:-------:|:-------:|:---------------|
  | `:r` | integer | 0-255   | red value      |
  | `:g` | integer | 0-255   | green value    |
  | `:b` | integer | 0-255   | blue value     |

  returns (long) - the relative luminance of the color"

  [{:keys [r g b]}]

  (let [normalFn    (fn [x] (/ x 255))
        linearizeFn (fn [x] (if (< x 0.03928)
                              (/ x 12.92)
                              (.pow js/Math (/ (+ x 0.055) 1.055) 2.4)))]
    (+ (* 0.2126 (linearizeFn (normalFn r)))
      (* 0.7152 (linearizeFn (normalFn g)))
      (* 0.0722 (linearizeFn (normalFn b))))))


(defn best-text-color
  "return `\"white\"` or `\"black\"` as the best color for text to be placed 'over'
  the given color.

  ---

  - rgba-color : (hash-map) containing:

  | key  | type    | range   | description    |
  |:-----|:-------:|:-------:|:---------------|
  | `:r` | integer | 0-255   | red value      |
  | `:g` | integer | 0-255   | green value    |
  | `:b` | integer | 0-255   | blue value     |
  | `:a` | float   | 0-1.0   | alpha channel  |

  return `\"white\"` or `\"black\"`
  "
  [rgba-color]

  (if (<= (relative-luminance rgba-color) 0.1833)
    "white"
    "black"))


(defn best-text-color-alpha
  "return `\"white\"` or `\"black\"` as the best color for text to be placed 'over'
  the given color, including it's [alpha channel](https://www.techopedia.com/definition/1945/alpha-channel).

  ---

  - rgba-color : (hash-map) containing:

  | key  | type    | range   | description    |
  |:-----|:-------:|:-------:|:---------------|
  | `:r` | integer | 0-255   | red value      |
  | `:g` | integer | 0-255   | green value    |
  | `:b` | integer | 0-255   | blue value     |
  | `:a` | float   | 0-1.0   | alpha channel  |

  return `\"white\"` or `\"black\"`
  "
  [{a :a :as rgba-color}]

  (if (<= (relative-luminance rgba-color) 0.1833)
    (if (<= 0.25 a) "white" "black")
    "black"))



(comment
  (-> "#ff0000" hex->rgba rgba-normal rgba-map->rgba-vector)
  (-> "#ff00ff" hex->rgba rgba-normal rgba-map->rgba-vector)

  (match-colors-hex "#000000")
  (match-colors-hex "#ff0000")
  (match-colors-hex "#ff00ff")

  (match-colors-rgba {:r 255 :g 0 :b 0 :a 1.0})



  (-> [1 0 0 1]
    normal->rgba
    rgba->hex)


  (.round js/Math (* 0.5 255))

  (def outline-color [1 0.5 0.78 1.0])
  (normal->rgba outline-color)
  (rgba->hex {:r 255, :g 127.5, :b 198.9, :a 1})
  (-> outline-color c/normal->rgba c/rgba->hex)
  ())


