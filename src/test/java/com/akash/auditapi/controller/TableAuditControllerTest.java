package com.akash.auditapi.controller;

import com.akash.auditapi.exception.ApiErrorCode;
import com.akash.auditapi.exception.GlobalExceptionHandler;
import com.akash.auditapi.exception.InvalidRequestException;
import com.akash.auditapi.model.SearchResponse;
import com.akash.auditapi.service.TableAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TableAuditControllerTest {
    private final TableAuditService service = mock(TableAuditService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TableAuditController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void returnsAllTableLabelsWithoutPagination() throws Exception {
        mockMvc.perform(get("/api/v1/allTable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").value("Request completed successfully"))
                .andExpect(jsonPath("$.data.tableLabels[0]").value("Holiday Calendar"))
                .andExpect(jsonPath("$.data.tableLabels[1]").value("Loco Singapore"))
                .andExpect(jsonPath("$.data.tableLabels[2]").value("Position Balance"))
                .andExpect(jsonPath("$.traceId").doesNotExist());
        verifyNoInteractions(service);
    }

    @Test
    void returnsResponseEntityWithPaginationDefaults() throws Exception {
        SearchResponse response = new SearchResponse(
                "PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD", 0, 10,
                0, 0, 0, false, false, List.of());
        when(service.sourceWithAuditHistory("PMC_POSITION_BALANCE", 0, 10)).thenReturn(response);

        mockMvc.perform(get("/api/v1/PMC_POSITION_BALANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").value("Request completed successfully"))
                .andExpect(jsonPath("$.traceId").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.data.sourceTable").value("PMC_POSITION_BALANCE"))
                .andExpect(jsonPath("$.data.auditTable").value("PMC_POSITION_BALANCE_AUD"))
                .andExpect(jsonPath("$.data.rows").isArray());
    }

    @Test
    void returnsStableErrorContract() throws Exception {
        when(service.sourceWithAuditHistory("BAD", 0, 10)).thenThrow(
                new InvalidRequestException(ApiErrorCode.INVALID_TABLE_NAME, "invalid table"));

        mockMvc.perform(get("/api/v1/BAD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TABLE_NAME"))
                .andExpect(jsonPath("$.message").value("invalid table"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void returnsSafeErrorForInvalidParameterType() throws Exception {
        mockMvc.perform(get("/api/v1/PMC_POSITION_BALANCE").param("pageNo", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.details").isEmpty());
    }
}
