package app.risk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers the weather-risk refresh on a schedule. The schedule comes from {@code
 * app.risk-refresh.cron} so it is configuration, not business logic. Disabled in tests by setting
 * {@code app.risk-refresh.enabled=false}; the {@link RiskRefreshService} can still be called
 * directly.
 */
@Component
@ConditionalOnProperty(
    name = "app.risk-refresh.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RiskRefreshScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RiskRefreshScheduler.class);

  private final RiskRefreshService riskRefreshService;

  public RiskRefreshScheduler(RiskRefreshService riskRefreshService) {
    this.riskRefreshService = riskRefreshService;
  }

  @Scheduled(cron = "${app.risk-refresh.cron}", zone = "${app.risk-refresh.zone:UTC}")
  public void refresh() {
    LOGGER.info("Scheduled weather-risk refresh started");
    riskRefreshService.refreshAllParcels();
  }
}
