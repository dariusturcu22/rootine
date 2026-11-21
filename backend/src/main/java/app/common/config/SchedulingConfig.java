package app.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables Spring's scheduled task support, used by the weather-risk refresh job. */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
