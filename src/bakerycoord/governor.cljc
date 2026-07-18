(ns bakerycoord.governor
  "BakeryCoordGovernor — the independent food-safety/scope layer
  gating every bakery scheduling/logistics proposal an advisor may
  make for a bakers, pastry-cooks and confectionery makers crew. The
  governor never dispatches hardware itself, never performs baking or
  preparation work itself, and never finalizes a food-safety-clearance
  decision (e.g. declaring a batch fit for sale) or an
  allergen-labeling determination, and never overrides a shop safety
  officer's judgment — those are permanently out of this actor's
  scope and remain a shop safety officer's exclusive judgment
  (README's 'Robotics premise': this actor coordinates BAKERY
  SCHEDULING/LOGISTICS ONLY — it never performs baking or preparation
  work itself). Modeled on cloud-itonami-isco-7211's
  foundrycoord.governor for the closest hot-process/heat-exposure
  workshop-safety domain shape.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. baker provenance      — the crew member must be independently
                                verified/registered before any action.
    2. bakery provenance     — the bakery site must be independently
                                verified/registered before any action.
    3. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never performs baking work itself; it
                                only gates what the advisor may
                                coordinate).
    4. closed op-allowlist    — only :log-work-record,
                                :schedule-crew-operation,
                                :flag-safety-concern and
                                :coordinate-supply-order may ever be
                                proposed; anything else is refused.
    5. scope-excluded action  — any proposal to directly declare a
                                batch fit for sale, finalize a
                                food-safety-clearance decision, or
                                finalize an allergen-labeling
                                determination, or to override a shop
                                safety officer's judgment, is a hard,
                                permanent block (checked both against
                                the proposed :op and, defense-in-depth,
                                against the proposal's :rationale text
                                — matched as full finalization/
                                execution ACTION phrases such as
                                \"declare the batch fit for sale\" /
                                \"finalize the allergen-labeling
                                determination\" / \"override the shop
                                safety officer's judgment\", never as
                                bare nouns like \"baking\", \"allergen\"
                                or \"safety\", so the check can never
                                self-trip on the advisor's own routine
                                rationale text, e.g. \"logged work
                                record for baker …\" or \"scheduled
                                crew operation for oven-shift task …\"
                                or \"…routed for shop safety officer
                                review\" — all three legitimately
                                contain those bare nouns but none is a
                                finalization action, and all are
                                exercised by
                                `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off
  regardless of confidence):
    6. :op :flag-safety-concern (a burn-hazard / allergen-cross-
                                contamination / equipment-condition
                                concern always escalates to a human,
                                never auto-commits).
    7. :op :coordinate-supply-order above `supply-cost-threshold`.
    8. low confidence (< `confidence-floor`)."
  (:require [clojure.string :as str]
            [bakerycoord.store :as store]))

(def confidence-floor 0.6)
(def supply-cost-threshold 2000)

(def allowed-ops
  #{:log-work-record :schedule-crew-operation
    :flag-safety-concern :coordinate-supply-order})

;; Defense-in-depth: none of these ops are ever in `allowed-ops`
;; above, so they are already refused by the closed-allowlist check
;; below; they are named again here — as explicit finalization/
;; execution ACTIONS, never bare nouns — so a future allowlist edit
;; cannot silently re-open this specific out-of-scope path without
;; also touching this list.
(def ^:private scope-excluded-ops
  #{:declare-batch-fit-for-sale :finalize-food-safety-clearance
    :finalize-batch-clearance-decision
    :finalize-allergen-labeling-determination
    :override-shop-safety-officer-judgment
    :override-safety-officer-judgment})

;; Full finalization/execution ACTION phrases only — never bare nouns
;; ("baking", "allergen", "safety", "batch", "officer") — so this can
;; never match inside the mock advisor's own default rationale text
;; (which legitimately contains those bare nouns, e.g. "oven-shift
;; task" / "shop safety officer review"). See
;; `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
(def ^:private scope-excluded-phrases
  ["declare the batch fit for sale" "declare this batch fit for sale"
   "finalize the food-safety clearance" "finalize the food safety clearance"
   "finalize the batch clearance decision"
   "finalize the allergen-labeling determination"
   "finalize the allergen labeling determination"
   "override the shop safety officer's judgment"
   "override the shop safety officer judgment"
   "override shop safety officer judgment"])

(defn- contains-excluded-phrase? [s]
  (let [s (str/lower-case (or s ""))]
    (boolean (some #(str/includes? s %) scope-excluded-phrases))))

(defn- hard-violations [proposal baker-record bakery-record]
  (let [{:keys [op rationale]} proposal]
    (cond-> []
      (nil? baker-record)
      (conj {:rule :no-baker
             :detail "未登録 baker への提案は不可（baker record は独立して検証・登録済みでなければならない）"})

      (nil? bakery-record)
      (conj {:rule :no-bakery
             :detail "未登録 bakery への提案は不可（bakery record は独立して検証・登録済みでなければならない）"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor はベーカリー作業を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op
             :detail (str op " は closed op-allowlist に無い — 提案不可")})

      (or (contains? scope-excluded-ops op) (contains-excluded-phrase? rationale))
      (conj {:rule :scope-excluded-action
             :detail "食品安全適合判定（batch clearance）・アレルゲン表示確定・shop safety officer の判断の上書きは、この actor の権限外 — 常に永続ブロック"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `bakerycoord.store/Store`. Pure — never
  mutates the store, never dispatches a bakery operation."
  [request _context proposal store]
  (let [baker-record (store/baker store (:baker-id request))
        bakery-record (some->> (:bakery-id proposal) (store/bakery store))
        hard (hard-violations proposal baker-record bakery-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        supply-order-over-threshold?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:cost proposal))
             (> (:cost proposal) supply-cost-threshold))
        always-risky? (or (= :flag-safety-concern (:op proposal))
                           supply-order-over-threshold?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
