(ns praetor.core
  (:require [replikativ.crdt.cdvcs.realize :refer [head-value]]
            [replikativ.crdt.cdvcs.stage :as s]
            [hasch.core :refer [uuid]]
            [replikativ.stage :refer [create-stage! connect! subscribe-crdts!]]
            [replikativ.peer :refer [client-peer server-peer]]
            [kabel.platform :refer [create-http-kit-handler! start stop]]
            [konserve.memory :refer [new-mem-store]]
            [full.async :refer [<?? <? go-try go-loop-try]]
            [clojure.core.async :refer [chan go-loop go]]))


(def server-state
  {:address "127.0.0.1"
   :port-index 31744
   :nodes {}})


(defn new-port [state]
  (let [old-port (-> state deref :port-index)]
    (swap! state update :port-index inc)
    old-port))


(defn spawn
  "Spawn replikativ node"
  [{:keys [email name eval-fns description mem?] :as config} state]
  (let [id (uuid)
        uri ("ws://" (:address @state) ":" (new-port state))
        store (<?? (new-mem-store)) 
        err-ch (chan)
        peer (server-peer
              (create-http-kit-handler! uri err-ch)
              name
              store
              err-ch)
        node {:store store :error-channel err-ch :peer peer :uri uri :eval-functions eval-fns}]
    (swap! state assoc-in [:nodes id] node)
    id))


(defn start
  "Start replikativ peer"
  [state id]
  (start (get-in @state [:nodes id :peer])))

(defn stop
  "Stop replikativ peer"
  [state id]
  (stop (get-in @state [:nodes id :peer])))




(comment

  (def uri "ws://127.0.0.1:31744")

  (def cdvcs-id #uuid "8e9074a1-e3b0-4c79-8765-b6537c7d0c44")
  
  (def eval-fns
    {'(fn [_ new] (if (set? new) new #{new}))
     (fn [_ new] (if (set? new) new #{new}))
     'conj conj})

  (def server-store (<?? (new-mem-store)))

  (def err-ch (chan))

  (go-loop [e (<? err-ch)]
    (when e
      (println "ERROR:" e)
      (recur (<? err-ch))))

  (def server
    (server-peer
     (create-http-kit-handler! uri err-ch)
     "BULLDOG BACKEND"
     server-store
     err-ch))

  (start server)

  (stop server)

  (def client-store (<?? (new-mem-store)))

  (def client (client-peer "BULLDOG FRONTEND" client-store err-ch))

  (def stage (<?? (create-stage! "eve@replikativ.io" client err-ch)))

  (<?? (connect! stage uri))

  (<?? (s/create-cdvcs! stage :description "Blog articles" :id cdvcs-id))

  (<?? (s/transact stage ["eve@replikativ.io" cdvcs-id]
                   '(fn [_ new] (if (set? new) new #{new}))
                   {:author "foo"
                    :title "bar"
                    :id #uuid "3e7f5bf0-f821-461d-b446-629d8411e47e"
                    :abstract "bar baz qux"
                    :content "foo bar bar foo foo"}))

  (<?? (s/commit! stage {"eve@replikativ.io" #{cdvcs-id}}))

  (<?? (head-value client-store
                   eval-fns
                   (:state (get @(:state client-store) ["eve@replikativ.io" cdvcs-id]))))

  (<?? (s/transact stage ["eve@replikativ.io" cdvcs-id]
                   'conj
                   {:author "bar"
                    :title "baz"
                    :id #uuid "23d6ba41-57f2-4eca-87cc-62053cd78338"
                    :abstract "bar baz qux"
                    :content "foo bar bar foo foo"}))

  (<?? (s/commit! stage {"eve@replikativ.io" #{cdvcs-id}}))

  (<?? (s/transact stage ["eve@replikativ.io" cdvcs-id]
                   '(fn [_ new] (if (set? new) new #{new}))
                   {:author "konny"
                    :title "World domination"
                    :id #uuid "3e7f5bf0-f821-461d-b446-629d8411e47e"
                    :abstract "Time to kill all"
                    :content "blablablablabla"}))

  (<?? (s/commit! stage {"eve@replikativ.io" #{cdvcs-id}}))


  (<?? (head-value client-store
                   eval-fns
                   (:state (get @(:state client-store) ["eve@replikativ.io" cdvcs-id]))))
  
  (<?? (head-value server-store
                 eval-fns
                 (:state (get @(:state server-store) ["eve@replikativ.io" cdvcs-id]))))

)
