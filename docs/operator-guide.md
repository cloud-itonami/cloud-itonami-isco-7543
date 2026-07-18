# Operator Guide

## First Deployment

1. Define the operator's facility coverage and grader/tester intake process.
2. Define consent and purpose categories for grader/tester/facility records.
3. Run synthetic operating cases (sample-intake log entry, testing-
   session scheduling, supply coordination, anomaly-concern flagging).
4. Enable human-reviewed sign-off for `:high`/`:safety-critical`
   actions (all flagged anomaly concerns, above-threshold supply
   orders).
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- consent and disclosure log
- anomaly-escalation path (visible defect, out-of-tolerance reading
  reported by intake staff)
- provenance for all operating records (grader/tester and facility
  both independently registered, grader/tester record including
  certification status)
- human review for high-risk cases
- audit export for all gated actions
- a hard, unconditional block on any attempt to route the
  grading/testing evaluation itself, a quality-grade assignment, a
  pass/fail determination, or a quality-clearance decision, through
  this actor — those decisions stay a human grader/tester's exclusive,
  irreplaceable judgment end to end

## Certification

Certified operators must prove that the governor gates every
safety-critical robot action, that anomaly-concern risks escalate to
humans, and that no deployment configuration can route the
grading/testing evaluation itself, a quality-grade assignment, a
pass/fail determination, or a quality-clearance decision through this
actor.
