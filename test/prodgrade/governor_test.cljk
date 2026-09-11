(ns prodgrade.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [prodgrade.store :as store]
            [prodgrade.advisor :as advisor]
            [prodgrade.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-grader! st {:grader-id "grader-1" :name "Kobo Yamada" :certified? true})
    (store/register-facility! st {:facility-id "FAC-1" :name "Kobo Testing Lab" :max-supply-cost 2000})
    st))

(defn- op [op-kw & {:as extra}]
  (merge {:op op-kw :effect :propose :facility-id "FAC-1"
          :confidence 0.9 :stake :low}
         extra))

(def ^:private req {:grader-id "grader-1"})

(deftest ok-log-sample-record
  (let [st (fresh-store)
        v (governor/check req {} (op :log-sample-record) st)]
    (is (:ok? v))))

(deftest ok-schedule-testing-session
  (let [st (fresh-store)
        v (governor/check req {} (op :schedule-testing-session) st)]
    (is (:ok? v))))

(deftest ok-supply-order-at-threshold-boundary
  (testing "the supply-cost threshold escalate boundary is exclusive (over, not at)"
    (let [st (fresh-store)
          v (governor/check req {} (op :coordinate-supply-order :cost 2000) st)]
      (is (:ok? v)))))

(deftest hard-on-unregistered-grader
  (let [st (fresh-store)
        v (governor/check {:grader-id "nobody"} {} (op :log-sample-record) st)]
    (is (:hard? v))
    (is (some #(= :no-grader (:rule %)) (:violations v)))))

(deftest hard-on-unregistered-facility
  (let [st (fresh-store)
        v (governor/check req {} (op :log-sample-record :facility-id "FAC-ghost") st)]
    (is (:hard? v))
    (is (some #(= :no-facility (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (op :log-sample-record) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-op-outside-closed-allowlist
  (let [st (fresh-store)
        v (governor/check req {} (op :dispatch-grader) st)]
    (is (:hard? v))
    (is (some #(= :unknown-op (:rule %)) (:violations v)))))

(deftest hard-on-scope-excluded-op-perform-quality-evaluation
  (testing "performing the grading/testing evaluation itself is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :perform-quality-evaluation) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-conduct-quality-evaluation
  (testing "conducting the grading/testing evaluation itself is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :conduct-quality-evaluation) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-assign-quality-grade
  (testing "assigning a quality grade is a permanent block, never a routine op — the human grader/tester's exclusive judgment"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :assign-quality-grade) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-finalize-quality-grade-assignment
  (testing "finalizing a quality-grade assignment is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :finalize-quality-grade-assignment) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-determine-pass-fail-outcome
  (testing "determining the pass/fail outcome is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :determine-pass-fail-outcome) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-finalize-pass-fail-determination
  (testing "finalizing the pass/fail determination is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :finalize-pass-fail-determination) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-finalize-quality-clearance
  (testing "finalizing the quality clearance is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :finalize-quality-clearance) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-authorize-quality-clearance
  (testing "authorizing the quality clearance is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :authorize-quality-clearance) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-assign-the-quality-grade-even-with-allowlisted-op
  (testing "defense-in-depth: a rationale that itself attempts to assign the quality grade is blocked even if the op looks routine"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :schedule-testing-session)
                                           :rationale "recommend we assign the quality grade now")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-determine-the-pass-fail-outcome-even-with-allowlisted-op
  (testing "defense-in-depth: a rationale that itself attempts to determine the pass/fail outcome is blocked even if the op looks routine"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :log-sample-record)
                                           :rationale "we should determine the pass/fail outcome right away")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-finalize-the-quality-clearance
  (testing "defense-in-depth: a rationale attempting to finalize the quality clearance is blocked even if the op looks routine"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :log-sample-record)
                                           :rationale "finalize the quality clearance and proceed")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest always-escalates-anomaly-concern-even-at-high-confidence
  (testing "a sample-condition anomaly (visible defect, out-of-tolerance reading reported by intake staff) always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :flag-anomaly-concern :anomaly-type :out-of-tolerance-reading)
                                           :confidence 0.99)
                             st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-supply-order-above-threshold
  (let [st (fresh-store)
        v (governor/check req {} (assoc (op :coordinate-supply-order :cost 5000) :confidence 0.99) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (op :log-sample-record) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest default-mock-advisor-proposals-never-self-trip-on-scope-exclusion
  (testing "the governor's scope-exclusion term list must never match the mock advisor's own default rationale text for any allowlisted op — CLAUDE.md's known self-tripping bug pattern (rationale legitimately contains bare nouns like 'quality'/'grade'/'test'/'clearance', but never the full finalization-action phrases)"
    (let [st (fresh-store)
          adv (advisor/mock-advisor)
          ops [:log-sample-record :schedule-testing-session
               :flag-anomaly-concern :coordinate-supply-order]]
      (doseq [o ops]
        (let [request {:grader-id "grader-1" :op o :facility-id "FAC-1"
                        :stake :low :item "quality-grade candidate batch 44 lot A"
                        :anomaly-type :visible-defect :cost 500}
              proposal (advisor/-advise adv st request)
              v (governor/check request {} proposal st)]
          (is (not (:hard? v))
              (str o " proposal unexpectedly hard-blocked: " (:violations v))))))))
