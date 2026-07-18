(ns prodgrade.governor
  "ProdGradeGovernor — the independent safety/scope layer gating every
  sample-intake-logging/session-scheduling proposal an advisor may
  make for a product grader/tester crew. The governor never dispatches
  hardware itself, NEVER performs the grading/testing evaluation
  itself, NEVER records or proposes a quality-grade/pass-fail
  determination, and NEVER finalizes a quality-clearance decision —
  those are permanently out of this actor's scope and remain a human
  grader/tester's exclusive, irreplaceable judgment (README's
  'Robotics premise': this actor coordinates SAMPLE-INTAKE LOGGING AND
  SCHEDULING ONLY — it never grades, tests or clears quality itself).
  Modeled on cloud-itonami-isco-7515's foodtaste.governor (closest
  available structural reference at scaffold time — ISCO 7515 already
  establishes this exact zero-sensory/grading-judgment-authority
  pattern for a closely analogous occupation; same closed-allowlist +
  independently-verified-provenance + always-escalate-concern +
  content-based scope-exclusion shape).

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. grader provenance     — the grader/tester must be independently
                                verified/registered before any action.
    2. facility provenance   — the facility must be independently
                                verified/registered before any action.
    3. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never performs any grading/testing/
                                quality-clearance judgment itself; it
                                only gates what the advisor may
                                coordinate).
    4. closed op-allowlist    — only :log-sample-record,
                                :schedule-testing-session,
                                :flag-anomaly-concern and
                                :coordinate-supply-order may ever be
                                proposed; anything else is refused.
    5. scope-excluded action  — any proposal to perform the
                                grading/testing evaluation itself, to
                                assign or finalize a quality-grade
                                assignment, to determine or finalize a
                                pass/fail determination, or to finalize
                                or authorize a quality clearance, is a
                                hard, permanent block (checked both
                                against the proposed :op and,
                                defense-in-depth, against the
                                proposal's :rationale text — matched as
                                full finalization/execution ACTION
                                phrases such as \"assign the quality
                                grade\" / \"determine the pass/fail
                                outcome\" / \"finalize the quality
                                clearance\", never as bare nouns like
                                \"quality\", \"grade\", \"test\" or
                                \"clearance\", so the check can never
                                self-trip on the advisor's own routine
                                rationale text, e.g. \"logged sample
                                intake record for grader …\" or
                                \"scheduled testing session for quality
                                evaluation panel …\" or \"…routed for
                                the human grader/tester's attention\" —
                                all three legitimately contain those
                                bare nouns but none is a finalization
                                action, and all are exercised by
                                `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off
  regardless of confidence):
    6. :op :flag-anomaly-concern (a sample-condition anomaly — a
                                visible defect, an out-of-tolerance
                                reading reported by intake staff —
                                always escalates to a human
                                grader/tester, never auto-commits).
    7. :op :coordinate-supply-order above `supply-cost-threshold`.
    8. low confidence (< `confidence-floor`)."
  (:require [clojure.string :as str]
            [prodgrade.store :as store]))

(def confidence-floor 0.6)
(def supply-cost-threshold 2000)

(def allowed-ops
  #{:log-sample-record :schedule-testing-session
    :flag-anomaly-concern :coordinate-supply-order})

;; Defense-in-depth: none of these ops are ever in `allowed-ops`
;; above, so they are already refused by the closed-allowlist check
;; below; they are named again here — as explicit finalization/
;; execution ACTIONS, never bare nouns — so a future allowlist edit
;; cannot silently re-open this specific out-of-scope path without
;; also touching this list. Four categories: performing the
;; grading/testing evaluation itself, assigning/finalizing a
;; quality-grade assignment, determining/finalizing a pass/fail
;; determination, and finalizing/authorizing a quality clearance —
;; every one of these is a human grader/tester's exclusive,
;; irreplaceable judgment.
(def ^:private scope-excluded-ops
  #{:perform-quality-evaluation
    :conduct-quality-evaluation
    :assign-quality-grade
    :finalize-quality-grade-assignment
    :determine-pass-fail-outcome
    :finalize-pass-fail-determination
    :finalize-quality-clearance
    :authorize-quality-clearance})

;; Full finalization/execution ACTION phrases only — never bare nouns
;; ("quality", "grade", "test", "clearance", "pass", "fail") — so this
;; can never match inside the mock advisor's own default rationale
;; text (which legitimately contains those bare nouns, e.g. "quality
;; evaluation panel" / "routed for the human grader/tester's
;; attention"). See
;; `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
(def ^:private scope-excluded-phrases
  ["perform the quality evaluation"
   "conduct the quality evaluation"
   "assign the quality grade"
   "finalize the quality grade assignment"
   "finalize the quality-grade assignment"
   "determine the pass/fail outcome"
   "determine the pass fail outcome"
   "finalize the pass/fail determination"
   "finalize the pass fail determination"
   "finalize the quality clearance"
   "finalize the quality-clearance"
   "authorize the quality clearance"
   "authorize the quality-clearance"])

(defn- contains-excluded-phrase? [s]
  (let [s (str/lower-case (or s ""))]
    (boolean (some #(str/includes? s %) scope-excluded-phrases))))

(defn- hard-violations [proposal grader-record facility-record]
  (let [{:keys [op rationale]} proposal]
    (cond-> []
      (nil? grader-record)
      (conj {:rule :no-grader
             :detail "未登録 grader への提案は不可（grader record は独立して検証・登録済み — certification status を含む — でなければならない）"})

      (nil? facility-record)
      (conj {:rule :no-facility
             :detail "未登録 facility への提案は不可（facility record は独立して検証・登録済みでなければならない）"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は grading/testing/quality-clearance 判断を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op
             :detail (str op " は closed op-allowlist に無い — 提案不可")})

      (or (contains? scope-excluded-ops op) (contains-excluded-phrase? rationale))
      (conj {:rule :scope-excluded-action
             :detail "grading/testing evaluation の実施・quality grade の確定・pass/fail 判定の確定・quality clearance の確定/許可は、この actor の権限外 — 常に永続ブロック（人間 grader/tester の専権）"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `prodgrade.store/Store`. Pure — never mutates
  the store, never dispatches a grading/testing/quality-clearance
  decision."
  [request _context proposal store]
  (let [grader-record (store/grader store (:grader-id request))
        facility-record (some->> (:facility-id proposal) (store/facility store))
        hard (hard-violations proposal grader-record facility-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        supply-order-over-threshold?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:cost proposal))
             (> (:cost proposal) supply-cost-threshold))
        always-risky? (or (= :flag-anomaly-concern (:op proposal))
                           supply-order-over-threshold?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
