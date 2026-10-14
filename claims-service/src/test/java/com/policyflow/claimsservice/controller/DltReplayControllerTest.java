package com.policyflow.claimsservice.controller;

import com.policyflow.claimsservice.replay.DltReplayService;
import com.policyflow.claimsservice.service.ReplayAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DltReplayController.class)
class DltReplayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DltReplayService dltReplayService;

    @MockitoBean
    private ReplayAuditService replayAuditService;

    @Test
    void invalidJsonShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/admin/dlt/replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-valid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        verify(replayAuditService).record(
                eq("UNKNOWN"), eq("UNKNOWN"),
                eq("REJECTED"), eq("Invalid JSON payload"));
    }

    @Test
    void validReplayShouldReturn200() throws Exception {
        String payload = """
                {
                  "eventId": "CONTROLLER-TEST-001",
                  "eventType": "CLAIM_CREATED",
                  "claimNumber": "CLM-CONTROLLER-001"
                }
                """;

        doNothing().when(dltReplayService).replay(payload);

        mockMvc.perform(post("/api/admin/dlt/replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(replayAuditService).record(
                eq("CONTROLLER-TEST-001"),
                eq("CLAIM_CREATED"),
                eq("PUBLISHED"),
                isNull());
    }

    @Test
    void kafkaPublicationFailureShouldReturn502() throws Exception {
        String payload = """
                {
                  "eventId": "CONTROLLER-TEST-002",
                  "eventType": "CLAIM_CREATED",
                  "claimNumber": "CLM-CONTROLLER-002"
                }
                """;

        doThrow(new ExecutionException(
                new RuntimeException("Simulated Kafka failure")))
                .when(dltReplayService).replay(payload);

        mockMvc.perform(post("/api/admin/dlt/replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("FAILED"));

        verify(replayAuditService).record(
                eq("CONTROLLER-TEST-002"),
                eq("CLAIM_CREATED"),
                eq("FAILED"),
                eq("Kafka publication failed"));
    }
}
