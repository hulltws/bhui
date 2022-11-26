(ns bh.ui-component.atom.card.flippable-card)


(def source-code '[:div.flip-card {:style (or style default-style)}
                   [:div.flip-card-inner
                    [:div.flip-card-front front]
                    [:div.flip-card-back back]]])


(def default-background "#9CA8B3")
(def default-color "#FF")
(def default-style {:width           "100%" :height "100%"
                    :overflow        "hidden"
                    :background      default-background
                    :color           default-color
                    :display         :flex
                    :backgroundSize  "contain"
                    :flex-direction  :column
                    :justify-content :center
                    :align-items     :center})
(def default-size {:width "100%" :height 400})

(defn card
  "returns a 'flippable card' react component (via [react-ui-cards](https://github.com/nukeop/react-ui-cards)).

  Works similar to [re-com](https://github.com/Day8/re-com) where the arguments are 'named` by keyword, but ***not***
  required to be inside a hash-map.
  We very cleverly combine Clojure's [variadics](https://clojure.org/guides/learn/functions#_variadic_functions) with
  [destructuring](https://clojure.org/guides/destructuring) to treat the 'exploded' key/value pairs
  as if they _had_ come from a hash-map...

  ---

  | key       | type     | description                                               |
  |:----------|:---------|-----------------------------------------------------------|
  | `:front`  | hiccup   | a reagent/react component, typically in [hiccup]() format for the front (unflipped) side of the card |
  | `:back`   | hiccup   | a reagent/react component, typically in [hiccup]() format for the back (flipped) side of the card |
  | `:style`  | hash-map | hash-map of any html/css style properties (minus the `:style` part itself, i.e., just the content part), typically used to specify the `:width` and `:height` of the card |

> Note: this library does NOT work properly on Webkit/Safari due to problems with the CSS. It
> should be possible to replace this library with just some `divs` and the proper CSS.
>
> See [here](https://line25.com/articles/super-cool-css-flip-effect-with-webkit-animation/) for an approach.
  "
  [& {:keys [front back style]}]

  [:div.flip-card {:style (or style default-style)}
   [:div.flip-card-inner
    [:div.flip-card-front front]
    [:div.flip-card-back back]]])
