package app.alert;

import app.risk.dto.RiskLevel;
import java.time.Instant;
import java.util.UUID;

/** The durable queue payload used to deliver one high-risk alert. */
public record AlertMessage(
    UUID userId,
    String recipientEmail,
    UUID parcelId,
    String parcelName,
    String crop,
    RiskLevel level,
    int riskIndex,
    String reason,
    Instant generatedAt) {}
