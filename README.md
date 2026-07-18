# cloud-itonami-isco-7512

Open Occupation Blueprint for **ISCO-08 7512**: Bakers, Pastry-cooks and Confectionery Makers.

This repository designs a forkable OSS business for a bakery scheduling and logistics coordination practice: a bakery scheduling and supply-coordination robot manages crew/task records under a governor-gated actor, so a bakers, pastry-cooks and confectionery makers crew keeps its own operating records instead of renting a closed workforce-management SaaS.

**Maturity: `:implemented`.** `src/bakerycoord/` implements the
`BakeryCoordActor` as a `langgraph.graph/state-graph`
(`bakerycoord.actor`) wired to a `Bakery Coordination Advisor`
(`bakerycoord.advisor`) and an independent `BakeryCoordGovernor`
(`bakerycoord.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. 21 tests / 45 assertions green (`clojure -M:test`).
HARD invariants (always hold, never overridable): baker provenance,
bakery provenance, no-actuation (`:effect` must be `:propose`), a closed
op-allowlist (`:log-work-record`, `:schedule-crew-operation`,
`:flag-safety-concern`, `:coordinate-supply-order` — nothing else may
ever be proposed), and a permanent, unconditional block on any
proposal that would directly declare a batch fit for sale, finalize a
food-safety-clearance decision, finalize an allergen-labeling
determination, or override a shop safety officer's judgment.
Always-escalate paths (human sign-off regardless of confidence,
mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-supply-order` above
the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a bakery scheduling/logistics coordination robot performs crew scheduling, batch/inventory/progress-record logging and baking-ingredients supply-order coordination for a bakers, pastry-cooks and confectionery makers crew, under an actor that proposes actions and an independent **Bakery Coordination Governor** that gates them. The governor never
dispatches hardware itself, never performs baking or preparation work on the bakery floor, and never finalizes a food-safety-clearance decision, an allergen-labeling determination, or overrides a shop safety officer's judgment; `:high`/`:safety-critical` actions (such as a flagged burn-hazard/allergen-cross-contamination/equipment-condition concern, or an above-threshold supply order) require human sign-off. **This actor coordinates bakery scheduling/logistics only — it never performs baking or preparation work, and never makes food-safety-clearance or allergen-labeling decisions itself.**

## Core Contract

```text
crew roster + bakery registration + safety-reporting policy
        |
        v
Bakery Coordination Advisor -> Bakery Coordination Governor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, declare
a batch fit for sale, finalize a food-safety-clearance decision, finalize an
allergen-labeling determination, override a shop safety officer's judgment,
suppress an operating record, or disclose sensitive data without governor
approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7512`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
