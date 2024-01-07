(ns ring-app.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.http-response :as response]
            [muuntaja.middleware :as muuntaja]
            [ring.middleware.reload :refer [wrap-reload]]
            [reitit.ring :as reitit]))



(defn html-handler [request-map]
  (response/ok
    (str "<html><body> your IP is: "
         (:remote-addr request-map)
         "</body></html>")))

(defn json-handler [request]
  (response/ok
    {:result (get-in request [:body-params :id])}))

(def routes
  [["/" {:get html-handler
         :post html-handler}]
   ["/echo/:id" {:get
                 (fn [{{:keys [id]} :path-params}]
                   (response/ok (str "<p>the value is: " id "</p>")))}]
   ["/api" {:middleware [muuntaja/wrap-format]}
    ["/multiply" {:post
                  (fn [{{:keys [a b]} :body-params}]
                    (response/ok {:result (* a b)}))}]]])

(def handler
  (reitit/ring-handler
    (reitit/router routes)
    (reitit/routes
      (reitit/create-resource-handler {:path "/"})
      (reitit/create-default-handler
        {:not-found
         (constantly (response/not-found "404 - Page Not Found"))
         :method-not-allowed
         (constantly (response/method-not-allowed "405 - Not allowed"))
         :not-acceptable
         (constantly (response/not-acceptable "406 - Not acceptable"))}))))

(defn wrap-nocache [handler]
  (fn [request]
    (-> request
        handler
        (assoc-in [:headers "Pragma"] "no-cache"))))

(defn -main []
  (jetty/run-jetty
    (-> #'handler
        wrap-nocache
        wrap-reload)
    {:port 3000
     :join? false}))