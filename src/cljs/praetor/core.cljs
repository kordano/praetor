(ns praetor.core
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn controls [data]
  (om/component
   (html
    [:ul
     [:li [:button {:onClick (fn [e] (.log js/console "Start local stage"))} "Start"]]
     [:li [:button {:onClick (fn [e] (.log js/console "Subscribe to server peer"))} "Subscribe"]]])))

(om/root
 controls
 {}
 {:target (. js/document (getElementById "app"))})
