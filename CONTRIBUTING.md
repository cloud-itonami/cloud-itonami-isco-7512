# Contributing

`cloud-itonami-isco-7512` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
kbb -M:dev:test
kbb -M:lint
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real baker, bakery or operator data, credentials or operating
  documents.
- Keep production writes and disclosures behind Bakery Coordination Governor.
- Treat this occupation's workflows as high-risk: add tests for permission,
  scope-exclusion, safety-escalation and audit logging.
- Never widen the closed op-allowlist to include a batch fit-for-sale
  declaration, a food-safety-clearance op, an allergen-labeling-
  determination op, or a shop-safety-officer-override op, without a
  dedicated ADR and explicit human review.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
