(ns prodgrade.store
  "SSoT for the ISCO-08 7543 Product Graders and Testers (excluding
  Foods and Beverages) sample-intake-logging/session-scheduling
  coordination actor (itonami actor pattern, ADR-2607121000 /
  CLAUDE.md Actors section; README's 'Robotics premise' — a
  sample-intake-logging/scheduling coordination robot performs sample
  intake/batch-identifier metadata logging, testing-session scheduling
  and testing-supply coordination for a human grader/tester crew under
  this advisor/governor pair, which never dispatches hardware itself,
  NEVER performs the grading/testing evaluation itself, NEVER records
  or proposes a quality-grade/pass-fail determination, and NEVER
  finalizes a quality-clearance decision — those remain a human
  grader/tester's exclusive, irreplaceable judgment end to end).
  Modeled on cloud-itonami-isco-7515's foodtaste.store (closest
  available structural reference at scaffold time — ISCO 7515 already
  establishes this exact zero-sensory/grading-judgment-authority
  pattern for a closely analogous occupation; same closed-allowlist +
  independently-verified-provenance + always-escalate-concern shape,
  generalized from food-specific framing (sample-intake,
  tasting-session) to non-food product grading/testing (sample-intake,
  testing-session)).

  Domain:

    grader    — a registered human product grader or tester
                {:grader-id :name :certified?}. `:certified?` is
                informational registered data (the grader/tester's
                certification status as recorded at registration
                time) — the governor's provenance check only requires
                the grader record to exist (independently verified/
                registered before any action); it never lets a
                proposal override or bypass the certification
                requirement itself, and never lets a proposal
                substitute for, override or bypass the grader/tester's
                own grading/testing judgment (see prodgrade.governor's
                scope-excluded-action rule).
    facility  — a registered testing facility/lab account
                {:facility-id :name :max-supply-cost}.
                `:max-supply-cost` is an informational registered
                ceiling used only to decide whether a
                `:coordinate-supply-order` proposal escalates to human
                sign-off (the governor never blocks a within-threshold
                order outright; it only decides commit vs. escalate).
    record    — a committed operating record (a logged sample-intake
                entry, a scheduled testing session, a flagged anomaly
                concern, or a coordinated supply order) — written ONLY
                via commit-record!. NEVER a grade/pass-fail outcome or
                a quality-clearance decision — no such record shape
                exists anywhere in this actor.
    ledger    — append-only audit trail, commit or hold.")

(defprotocol Store
  (grader [s grader-id])
  (facility [s facility-id])
  (records-of [s grader-id])
  (ledger [s])
  (register-grader! [s g])
  (register-facility! [s f])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (grader [_ grader-id] (get-in @a [:graders grader-id]))
  (facility [_ facility-id] (get-in @a [:facilities facility-id]))
  (records-of [_ grader-id] (filter #(= grader-id (:grader-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-grader! [s g]
    (swap! a assoc-in [:graders (:grader-id g)] g) s)
  (register-facility! [s f]
    (swap! a assoc-in [:facilities (:facility-id f)] f) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:graders {} :facilities {} :records [] :ledger []}
                                    seed)))))
