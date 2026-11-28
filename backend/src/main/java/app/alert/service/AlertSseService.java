package app.alert.service;

import app.alert.dto.AlertEvent;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Tracks authenticated streams and sends each alert only to its owner's connected clients. */
@Service
public class AlertSseService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AlertSseService.class);

  private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByUser =
      new ConcurrentHashMap<>();

  public SseEmitter subscribe(UUID userId) {
    SseEmitter emitter = new SseEmitter(0L);
    CopyOnWriteArrayList<SseEmitter> emitters =
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>());
    emitters.add(emitter);
    emitter.onCompletion(() -> removeEmitter(userId, emitters, emitter));
    emitter.onTimeout(() -> removeEmitter(userId, emitters, emitter));
    emitter.onError(error -> removeEmitter(userId, emitters, emitter));
    return emitter;
  }

  public void publish(UUID userId, AlertEvent event) {
    CopyOnWriteArrayList<SseEmitter> emitters = emittersByUser.get(userId);
    if (emitters == null) {
      return;
    }
    for (SseEmitter emitter : emitters) {
      send(userId, emitter, event);
    }
  }

  private void send(UUID userId, SseEmitter emitter, AlertEvent event) {
    try {
      emitter.send(SseEmitter.event().name("risk-alert").data(event));
    } catch (IOException | IllegalStateException exception) {
      LOGGER.debug("Removing disconnected risk-alert stream for user {}", userId, exception);
      emitter.completeWithError(exception);
    }
  }

  private void removeEmitter(
      UUID userId, CopyOnWriteArrayList<SseEmitter> emitters, SseEmitter emitter) {
    emittersByUser.computeIfPresent(
        userId,
        (key, current) -> {
          if (current != emitters) {
            return current;
          }
          current.remove(emitter);
          return current.isEmpty() ? null : current;
        });
  }
}
