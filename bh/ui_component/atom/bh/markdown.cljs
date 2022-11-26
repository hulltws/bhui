(ns bh.ui-component.atom.bh.markdown
  (:require ["react-markdown" :as ReactMarkdown]))


(defn markdown [content]
  [:> ReactMarkdown {:source content}])