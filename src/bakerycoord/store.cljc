(ns bakerycoord.store
  "SSoT for the ISCO-08 7512 bakers, pastry-cooks and confectionery
  makers bakery scheduling/logistics coordination actor (itonami
  actor pattern, ADR-2607121000 / CLAUDE.md Actors section; README's
  'Robotics premise' — a bakery scheduling/logistics coordination
  robot performs crew scheduling, batch/inventory/progress record
  logging and baking-ingredients supply-order coordination for a
  bakery, pastry and confectionery crew under this advisor/governor
  pair, which never dispatches hardware itself, never performs
  baking or preparation work itself, and never finalizes a
  food-safety-clearance decision or an allergen-labeling
  determination, nor overrides a shop safety officer's judgment —
  those remain the shop safety officer's exclusive judgment). Modeled
  on cloud-itonami-isco-7211's foundrycoord.store (closest
  hot-process/heat-exposure workshop-safety domain shape).

  Domain:

    baker   — a registered baker/pastry-cook/confectionery crew
              member (:baker-id, :name)
    bakery  — a registered bakery site {:bakery-id :name
              :max-supply-cost number}. `:max-supply-cost` is an
              informational registered ceiling used only to decide
              whether a `:coordinate-supply-order` proposal escalates
              to human sign-off (the governor never blocks a
              within-threshold order outright; it only decides
              commit vs. escalate).
    record  — a committed operating record (a logged batch/inventory/
              progress entry, a scheduled crew/oven-shift operation,
              a flagged safety concern, or a coordinated
              baking-ingredients supply order) — written ONLY via
              commit-record!.
    ledger  — append-only audit trail, commit or hold.")

(defprotocol Store
  (baker [s baker-id])
  (bakery [s bakery-id])
  (records-of [s baker-id])
  (ledger [s])
  (register-baker! [s baker])
  (register-bakery! [s bakery])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (baker [_ baker-id] (get-in @a [:bakers baker-id]))
  (bakery [_ bakery-id] (get-in @a [:bakeries bakery-id]))
  (records-of [_ baker-id] (filter #(= baker-id (:baker-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-baker! [s b]
    (swap! a assoc-in [:bakers (:baker-id b)] b) s)
  (register-bakery! [s bk]
    (swap! a assoc-in [:bakeries (:bakery-id bk)] bk) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:bakers {} :bakeries {} :records [] :ledger []}
                                    seed)))))
