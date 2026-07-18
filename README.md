# cloud-itonami-isco-7543

Open Occupation Blueprint for **ISCO-08 7543**: Product Graders and Testers (excluding Foods and Beverages).

This repository designs a forkable OSS business for a non-food product grading/testing practice: a sample-intake-logging and session-scheduling coordination robot manages grader/tester and facility records under a governor-gated actor, so a grader/tester crew keeps its own operating records instead of renting a closed quality-management SaaS.

**Maturity: `:implemented`.** `src/prodgrade/` implements the
`ProdGradeActor` as a `langgraph.graph/state-graph`
(`prodgrade.actor`) wired to a `Product Grader/Tester
Advisor` (`prodgrade.advisor`) and an independent `ProdGradeGovernor`
(`prodgrade.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok? true) +-> :request-approval (:escalate? true, human-in-the-loop interrupt)
+-> :hold (:hard? true)`. HARD invariants (always hold, never
overridable): grader/tester provenance, facility provenance, no-actuation
(`:effect` must be `:propose`), a closed op-allowlist
(`:log-sample-record`, `:schedule-testing-session`,
`:flag-anomaly-concern`, `:coordinate-supply-order` — nothing else may
ever be proposed), and a permanent, unconditional block on any
proposal that would perform the grading/testing evaluation itself,
assign or finalize a quality-grade assignment, determine or finalize a
pass/fail determination, or finalize or authorize a quality clearance.
Always-escalate paths (human sign-off regardless of confidence,
mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-anomaly-concern` (always) and `:coordinate-supply-order` above
the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a sample-intake-logging and session-scheduling
coordination robot performs sample-intake/batch-identifier metadata logging,
testing-session scheduling and testing-supply coordination for a product
grader/tester crew, under an actor that proposes actions and an
independent **ProdGradeGovernor** that gates them. The governor never
dispatches hardware itself, NEVER performs the grading/testing
evaluation itself, NEVER records or proposes a quality-grade/pass-fail
determination, and NEVER finalizes a quality-clearance decision; `:high`/
`:safety-critical` actions (such as a flagged sample-condition anomaly, or an
above-threshold supply order) require human sign-off. **This actor
coordinates sample-intake logging and session scheduling only — it never
performs, records or finalizes any grading/testing/quality-clearance
judgment itself; that judgment always requires a human grader/tester's
own evaluation.**

## Core Contract

```text
grader/tester roster + facility registration + anomaly-reporting policy
        |
        v
Product Grader/Tester Advisor -> ProdGradeGovernor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, perform
the grading/testing evaluation itself, assign or finalize a quality-grade
assignment, determine or finalize a pass/fail determination, finalize or
authorize a quality clearance, suppress an operating record, or disclose
sensitive data without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7543`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation

`src/prodgrade/{store,advisor,governor,actor}.cljc` is a real
implementation of the Core Contract above, pure `.cljc` (portable
across JVM/cljs/WASM per this workspace's runtime-priority rules — no
JVM-only interop). Modeled on cloud-itonami-isco-7515's `foodtaste.*`
(ISCO 7515, Food and Beverage Tasters and Graders, already landed —
closest structurally analogous occupation and the established
precedent for this actor's zero-sensory/grading-judgment-authority
pattern: same closed-allowlist + independently-verified-provenance +
always-escalate-concern + content-based scope-exclusion shape,
generalized from food-specific framing (sample-intake,
tasting-session) to non-food product grading/testing (sample-intake,
testing-session)).

```bash
clojure -M:test   # full suite, green
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ADR-2607121000, Wave3 production/trades).

## License

AGPL-3.0-or-later.
