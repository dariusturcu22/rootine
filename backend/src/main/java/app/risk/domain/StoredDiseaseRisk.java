package app.risk.domain;

/**
 * One disease's risk, stored inside the {@code parcel_risk.diseases} JSON document. Levels are kept
 * as the AI service's lowercase strings so the stored data matches the source exactly.
 *
 * @param disease the disease name, for example {@code late_blight}
 * @param model the weather rule model, for example {@code hutton}
 * @param level the risk level as a lowercase string
 * @param riskIndex the risk score, from 0 to 100
 */
public record StoredDiseaseRisk(String disease, String model, String level, int riskIndex) {}
