(ns prodgrade.actor
  "ProdGradeActor — the ISCO-08 7543 Product Graders and Testers
  (excluding Foods and Beverages) sample-intake-logging/session-
  scheduling coordination actor as a `langgraph.graph/state-graph`
  (ADR-2607121000 / CLAUDE.md Actors section). One graph run = one
  coordination operation request (intake -> advise -> govern -> decide
  -> commit/hold, with a human-approval interrupt for escalated
  proposals). No infinite internal loop; checkpointed per superstep so
  an interrupted run can resume after human sign-off. Modeled on
  cloud-itonami-isco-7515's foodtaste.actor (closest available
  structural reference at scaffold time — ISCO 7515 already
  establishes this exact zero-sensory/grading-judgment-authority
  pattern for a closely analogous occupation; same closed-allowlist +
  independently-verified-provenance + always-escalate-concern shape).

  ```text
  :intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                             +-> :request-approval   (:escalate? true, interrupt-before)
                                             +-> :hold               (:hard? true)
  ```

  The unconditional invariant: the Product Grader/Tester Advisor can
  never directly commit an operating record the ProdGradeGovernor
  refuses, and can never make this actor perform the grading/testing
  evaluation itself, record or finalize a quality-grade/pass-fail
  determination, or finalize a quality-clearance decision — every
  commit-record! call is gated behind `:decide`, and that decision
  class is a permanent hard block regardless of disposition path. This
  actor coordinates SAMPLE-INTAKE LOGGING AND SCHEDULING ONLY — it
  never performs, records or finalizes any grading/testing/quality-
  clearance judgment itself; that judgment always requires a human
  grader/tester's own evaluation."
  (:require [langgraph.graph :as g]
            [langgraph.checkpoint :as cp]
            [prodgrade.advisor :as advisor]
            [prodgrade.governor :as governor]
            [prodgrade.store :as store]))

(defn build-graph
  "Build a compiled ProdGradeActor graph. `store` implements
  `prodgrade.store/Store`. `advisor` implements
  `prodgrade.advisor/Advisor` (defaults to `mock-advisor`).
  `checkpointer` defaults to an in-memory one."
  [{:keys [store advisor checkpointer]
    :or {advisor (advisor/mock-advisor)
         checkpointer (cp/mem-checkpointer)}}]
  (-> (g/state-graph
       {:channels
        {:request     {:default nil}
         :context     {:default nil}
         :proposal    {:default nil}
         :verdict     {:default nil}
         :disposition {:default nil}
         :record      {:default nil}
         :audit       {:reducer into :default []}}})
      (g/add-node :intake (fn [s] s))
      (g/add-node :advise
                   (fn [{:keys [request]}]
                     (let [p (advisor/-advise advisor store request)]
                       {:proposal p
                        :audit [{:node :advise :request request :proposal p}]})))
      (g/add-node :govern
                   (fn [{:keys [request context proposal]}]
                     (let [v (governor/check request context proposal store)]
                       {:verdict v
                        :audit [{:node :govern :verdict v}]})))
      (g/add-node :decide
                   (fn [{:keys [verdict]}]
                     {:disposition (cond
                                     (:hard? verdict) :hold
                                     (:escalate? verdict) :request-approval
                                     :else :commit)}))
      (g/add-node :request-approval (fn [s] s))
      (g/add-node :commit
                   (fn [{:keys [request proposal]}]
                     (let [record {:grader-id (:grader-id request)
                                    :op (:op proposal)
                                    :facility-id (:facility-id proposal)
                                    :payload proposal}]
                       (store/commit-record! store record)
                       (store/append-ledger! store {:disposition :commit :record record})
                       {:record record
                        :audit [{:node :commit :record record}]})))
      (g/add-node :hold
                   (fn [{:keys [verdict]}]
                     (store/append-ledger! store {:disposition :hold :verdict verdict})
                     {:audit [{:node :hold :verdict verdict}]}))
      (g/set-entry-point :intake)
      (g/add-edge :intake :advise)
      (g/add-edge :advise :govern)
      (g/add-edge :govern :decide)
      (g/add-conditional-edges
       :decide
       (fn [{:keys [disposition]}]
         (case disposition
           :commit :commit
           :request-approval :request-approval
           :hold)))
      (g/add-edge :request-approval :commit)
      (g/set-finish-point :commit)
      (g/set-finish-point :hold)
      (g/compile-graph {:checkpointer checkpointer
                         :interrupt-before #{:request-approval}})))

(defn run-request!
  "Run one operation request to completion or interrupt. `thread-id`
  scopes checkpointing for resume after human approval."
  [graph request context thread-id]
  (g/run* graph {:request request :context context} {:thread-id thread-id}))

(defn approve!
  "Human-in-the-loop resume: the interrupted `:request-approval` node
  advances straight to `:commit` on resume (approval is the act of
  resuming the thread)."
  [graph thread-id]
  (g/run* graph nil {:thread-id thread-id :resume? true}))
