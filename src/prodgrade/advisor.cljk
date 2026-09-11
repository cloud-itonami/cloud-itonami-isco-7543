(ns prodgrade.advisor
  "Product Grader/Tester Advisor — proposing a
  sample-intake-logging/scheduling coordination operation (log a
  sample-intake/batch-identifier record, schedule a testing session,
  flag a sample-condition anomaly, coordinate a testing-supply order)
  from a grader/tester roster, facility registration and
  anomaly-reporting policy. Swappable mock/llm; the advisor ONLY
  proposes — `prodgrade.governor` independently gates every proposal
  and always escalates anomaly concerns and above-threshold supply
  orders. The advisor never proposes to perform the grading/testing
  evaluation itself, to directly assign the quality grade, to
  determine the pass/fail outcome, or to finalize the quality
  clearance — those stay permanently out of this actor's scope and
  remain a human grader/tester's exclusive judgment. Modeled on
  cloud-itonami-isco-7515's foodtaste.advisor (closest available
  structural reference at scaffold time).

  A proposal: {:op :log-sample-record|:schedule-testing-session|
               :flag-anomaly-concern|:coordinate-supply-order
               :effect :propose :grader-id str :facility-id str
               :cost number :anomaly-type kw :item str :stake kw
               :confidence n :rationale str}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- rationale-for [op grader-id facility-id anomaly-type]
  (case op
    :log-sample-record
    (str "logged sample intake record for grader " grader-id
         " at facility " facility-id)

    :schedule-testing-session
    (str "scheduled testing session for quality evaluation panel at facility "
         facility-id)

    :flag-anomaly-concern
    (str "flagged " (name (or anomaly-type :anomaly)) " concern for grader "
         grader-id " at facility " facility-id
         " — routed for the human grader/tester's attention")

    :coordinate-supply-order
    (str "coordinated supply order for grader " grader-id " at facility " facility-id)

    (str "proposed " (name op) " for grader " grader-id " at facility " facility-id)))

(defn- infer [_store {:keys [op stake grader-id facility-id cost anomaly-type item]
                       :as request}]
  {:op op
   :effect :propose
   :grader-id grader-id
   :facility-id facility-id
   :cost cost
   :anomaly-type anomaly-type
   :item item
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (rationale-for op grader-id facility-id anomaly-type)})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a product grader/tester sample-intake-logging and
   session-scheduling coordination advisor. Given a request, propose
   an :op (one of :log-sample-record, :schedule-testing-session,
   :flag-anomaly-concern, :coordinate-supply-order), the :grader-id,
   :facility-id, and any :cost/:anomaly-type/:item fields, an honest
   :confidence and a :stake. Never propose an op outside this closed
   list, and never propose to perform the grading/testing evaluation
   itself, to directly assign the quality grade, to determine the
   pass/fail outcome, or to finalize the quality clearance — those are
   always out of this actor's scope; it coordinates sample-intake
   logging and session scheduling only and never performs, records or
   finalizes any grading/testing/quality-clearance judgment itself.
   Anomaly concerns always require human sign-off regardless of
   confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
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
