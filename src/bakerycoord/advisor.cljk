(ns bakerycoord.advisor
  "Bakery Coordination Advisor — proposing a bakery
  scheduling/logistics coordination operation (log a work record,
  schedule a crew operation, flag a safety concern, coordinate a
  baking-ingredients supply order) from a crew roster, bakery
  registration and safety-reporting policy. Swappable mock/llm; the
  advisor ONLY proposes — `bakerycoord.governor` independently gates
  every proposal and always escalates safety concerns and
  above-threshold supply orders. The advisor never proposes to
  directly declare a batch fit for sale, finalize a
  food-safety-clearance decision, or finalize an allergen-labeling
  determination, nor to override a shop safety officer's judgment —
  those stay permanently out of this actor's scope. Modeled on
  cloud-itonami-isco-7211's foundrycoord.advisor for the closest
  hot-process/heat-exposure workshop-safety domain shape.

  A proposal: {:op :log-work-record|:schedule-crew-operation|
               :flag-safety-concern|:coordinate-supply-order
               :effect :propose :baker-id str :bakery-id str
               :cost number :hazard-type kw :task str :stake kw
               :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- rationale-for [op baker-id bakery-id hazard-type]
  (case op
    :log-work-record
    (str "logged work record for baker " baker-id " at bakery " bakery-id)

    :schedule-crew-operation
    (str "scheduled crew operation for oven-shift task at bakery " bakery-id)

    :flag-safety-concern
    (str "flagged " (name (or hazard-type :hazard)) " concern for baker "
         baker-id " at bakery " bakery-id " — routed for shop safety officer review")

    :coordinate-supply-order
    (str "coordinated supply order for baker " baker-id " at bakery " bakery-id)

    (str "proposed " (name op) " for baker " baker-id " at bakery " bakery-id)))

(defn- infer [_store {:keys [op stake baker-id bakery-id cost hazard-type task]
                       :as request}]
  {:op op
   :effect :propose
   :baker-id baker-id
   :bakery-id bakery-id
   :cost cost
   :hazard-type hazard-type
   :task task
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (rationale-for op baker-id bakery-id hazard-type)})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a bakers, pastry-cooks and confectionery makers bakery
   scheduling/logistics coordination advisor. Given a request,
   propose an :op (one of :log-work-record, :schedule-crew-operation,
   :flag-safety-concern, :coordinate-supply-order), the :baker-id,
   :bakery-id, and any :cost/:hazard-type/:task fields, an honest
   :confidence and a :stake. Never propose an op outside this closed
   list, and never propose to directly declare a batch fit for sale,
   finalize a food-safety-clearance decision, or finalize an
   allergen-labeling determination, nor to override a shop safety
   officer's judgment — those are always out of this actor's scope;
   it coordinates bakery scheduling/logistics only and never performs
   baking or preparation work or makes food-safety-clearance or
   allergen-labeling decisions itself. Safety concerns always require
   human sign-off regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
