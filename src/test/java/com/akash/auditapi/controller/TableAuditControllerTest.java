package com.akash.auditapi.controller;

import com.akash.auditapi.enums.ApiOutcomeCode;
import com.akash.auditapi.exceptionHandlers.GlobalExceptionHandler;
import com.akash.auditapi.exception.InvalidRequestException;
import com.akash.auditapi.dto.response.SearchResponse;
import com.akash.auditapi.service.TableAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
        when(service.findAllTableLabels()).thenReturn(List.of(
                "Account-Statement", "Loco-Singapore", "Position-Balance"));

        mockMvc.perform(get("/api/v1/allTable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value("2000"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").value("Request completed successfully"))
                .andExpect(jsonPath("$.httpStatus").doesNotExist())
                .andExpect(jsonPath("$.data.tableLabels[0]").value("Account-Statement"))
                .andExpect(jsonPath("$.data.tableLabels[1]").value("Loco-Singapore"))
                .andExpect(jsonPath("$.data.tableLabels[2]").value("Position-Balance"));
        verify(service).findAllTableLabels();
    }

    @Test
    void returnsResponseEntityWithPaginationDefaults() throws Exception {
        SearchResponse response = new SearchResponse(
                0, 10, 0, 0, 0, false, false, List.of());
        when(service.sourceWithAuditHistory("POSITION_BALANCE", 0, 10)).thenReturn(response);

        mockMvc.perform(get("/api/v1/POSITION_BALANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value("2000"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").value("Request completed successfully"))
                .andExpect(jsonPath("$.httpStatus").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.data.sourceTable").doesNotExist())
                .andExpect(jsonPath("$.data.auditTable").doesNotExist())
                .andExpect(jsonPath("$.data.rows").isArray());
    }

    @Test
    void returnsStableErrorContract() throws Exception {
        when(service.sourceWithAuditHistory("BAD", 0, 10)).thenThrow(
                new InvalidRequestException(ApiOutcomeCode.INVALID_TABLE_NAME, "invalid table"));

        mockMvc.perform(get("/api/v1/BAD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_TABLE_NAME"))
                .andExpect(jsonPath("$.code").value("4000"))
                .andExpect(jsonPath("$.message").value("invalid table"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.httpStatus").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void returnsSafeErrorForInvalidParameterType() throws Exception {
        mockMvc.perform(get("/api/v1/POSITION_BALANCE").param("pageNo", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.code").value("4005"))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.details").isEmpty());
    }
}
