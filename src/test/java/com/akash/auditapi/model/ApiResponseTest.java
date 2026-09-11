package com.akash.auditapi.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {
    @Test
    void wrapsPayloadWithSharedSuccessMetadata() {
        TableLabelsResponse data = new TableLabelsResponse(List.of("Holiday Calendar"));
        ApiResponse<TableLabelsResponse> response = ApiResponse.success(
                data, "Request completed successfully");

        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getCode()).isEqualTo(ApiOutcomeCode.SUCCESS);
        assertThat(response.getMessage()).isEqualTo("Request completed successfully");
        assertThat(response.getData()).isSameAs(data);
    }
}
