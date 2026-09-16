package com.akash.auditapi.dto.response;

import com.akash.auditapi.enums.ApiOutcomeCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ApiResponseTest {
    @Test
    void wrapsPayloadWithSharedSuccessMetadata() {
        TableLabelsResponse data = new TableLabelsResponse(List.of("Account-Statement"));
        ApiResponse<TableLabelsResponse> response = ApiResponse.success(
                data, "Request completed successfully");

        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ApiOutcomeCode.SUCCESS);
        assertThat(response.getCode()).isEqualTo("2000");
        assertThat(response.getMessage()).isEqualTo("Request completed successfully");
        assertThat(response.getData()).isSameAs(data);
    }

    @Test
    void definesStableApplicationCodeForEveryStatus() {
        assertThat(ApiOutcomeCode.values())
                .extracting(Enum::name, ApiOutcomeCode::applicationCode)
                .containsExactly(
                        tuple("SUCCESS", "2000"),
                        tuple("REDIRECTION", "3000"),
                        tuple("INVALID_TABLE_NAME", "4000"),
                        tuple("INVALID_PAGE_NO", "4001"),
                        tuple("INVALID_PAGE_SIZE", "4002"),
                        tuple("AUDIT_TABLE_NOT_ACCEPTED", "4003"),
                        tuple("VALIDATION_FAILED", "4004"),
                        tuple("INVALID_REQUEST", "4005"),
                        tuple("TABLE_NOT_ALLOWED", "4007"),
                        tuple("TABLE_PAIR_NOT_FOUND", "4008"),
                        tuple("MISSING_REQUIRED_COLUMN", "4009"),
                        tuple("HOLIDAY_NOT_FOUND", "4010"),
                        tuple("INTERNAL_ERROR", "5000"),
                        tuple("DATABASE_ERROR", "5001"));
    }
}
