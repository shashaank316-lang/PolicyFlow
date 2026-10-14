
package com.policyflow.claimsservice.controller;

import com.policyflow.claimsservice.replay.DltReplayService;
import com.policyflow.claimsservice.service.ReplayAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/admin/dlt")
public class DltReplayController {

    private static final Logger logger =
            LoggerFactory.getLogger(DltReplayController.class);

    private final DltReplayService dltReplayService;
    private final ReplayAuditService replayAuditService;
    private final ObjectMapper objectMapper;

    public DltReplayController(
            DltReplayService dltReplayService,
            ObjectMapper objectMapper,
            ReplayAuditService replayAuditService) {
        this.dltReplayService = dltReplayService;
        this.objectMapper = objectMapper;
        this.replayAuditService = replayAuditService;
    }

    @PostMapping("/replay")
    public ResponseEntity<Map<String, String>> replay(
            @RequestBody String payload) {

        String eventId = "UNKNOWN";
        String eventType = "UNKNOWN";

        try {
            JsonNode event = objectMapper.readTree(payload);

            if (event != null && event.isObject()) {
                eventId = event.path("eventId").asString("UNKNOWN");
                eventType = event.path("eventType").asString("UNKNOWN");
            }
        } catch (JacksonException ex) {
            logger.warn("DLT replay rejected: invalid JSON");

            replayAuditService.record(
                    eventId,
                    eventType,
                    "REJECTED",
                    "Invalid JSON payload"
            );

            return ResponseEntity.badRequest().body(Map.of(
                    "status", "REJECTED",
                    "message", "Replay payload is not valid JSON"
            ));
        }

        try {
            logger.info(
                    "DLT replay requested: eventId={}, eventType={}",
                    eventId, eventType
            );

            dltReplayService.replay(payload);

            replayAuditService.record(
                    eventId, eventType, "PUBLISHED", null
            );

            logger.info(
                    "DLT replay published: eventId={}, eventType={}",
                    eventId, eventType
            );

            return ResponseEntity.ok(Map.of(
                    "status", "PUBLISHED",
                    "message", "Event successfully republished to claim-events"
            ));

        } catch (IllegalArgumentException ex) {
            logger.warn(
                    "DLT replay rejected: eventId={}, eventType={}, reason={}",
                    eventId, eventType, ex.getMessage()
            );

            replayAuditService.record(
                    eventId, eventType, "REJECTED", ex.getMessage()
            );

            return ResponseEntity.badRequest().body(Map.of(
                    "status", "REJECTED",
                    "message", ex.getMessage()
            ));

        } catch (TimeoutException | ExecutionException ex) {
            logger.error(
                    "DLT replay publication failed: eventId={}, eventType={}",
                    eventId, eventType, ex
            );

            replayAuditService.record(
                    eventId,
                    eventType,
                    "FAILED",
                    "Kafka publication failed"
            );

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "status", "FAILED",
                    "message", "Kafka did not confirm publication"
            ));

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            logger.error(
                    "DLT replay interrupted: eventId={}, eventType={}",
                    eventId, eventType, ex
            );

            replayAuditService.record(
                    eventId, eventType, "FAILED", "Replay was interrupted"
            );

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "status", "FAILED",
                            "message", "Replay was interrupted"
                    ));

        } catch (Exception ex) {
            logger.error(
                    "Unexpected DLT replay failure: eventId={}, eventType={}",
                    eventId, eventType, ex
            );

            replayAuditService.record(
                    eventId,
                    eventType,
                    "FAILED",
                    "Unexpected replay failure"
            );

            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "FAILED",
                    "message", "Replay failed due to an unexpected error"
            ));
        }
    }
}
