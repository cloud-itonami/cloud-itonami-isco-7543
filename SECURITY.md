# Security Policy

This project handles product grading/testing operating workflows.
Treat vulnerabilities as potentially high impact even when the demo data is
synthetic — this domain's failure modes include unsafe-product exposure and
consumer-safety incidents if a quality-clearance decision were ever routed
through an unqualified path.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real grader/tester, facility or operator data exposure
- authorization bypass
- ProdGradeGovernor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch
- any path that lets a proposal reach the grading/testing evaluation
  itself, a quality-grade assignment, a pass/fail determination, or a
  quality-clearance decision

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on grader/tester/facility data, policy enforcement or audit
  logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real grader/tester/facility/operator data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and facility accounts.
