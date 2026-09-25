package com.akash.auditapi.integration;

import com.akash.auditapi.dao.CommodityDebitRepository;
import com.akash.auditapi.dao.TableMetadataDao;
import com.akash.auditapi.entity.CommodityDebit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:public-api-e2e;MODE=Oracle;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class PublicApiEndToEndTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommodityDebitRepository commodityDebitRepository;

    @MockitoBean
    private TableMetadataDao tableMetadataDao;

    @BeforeEach
    void discoverTables() {
        commodityDebitRepository.deleteAll();
        when(tableMetadataDao.findSourceTables("PMC_", "_AUD")).thenReturn(List.of(
                "PMC_ACCOUNT_STATEMENT", "PMC_LOCO_SINGAPORE", "PMC_POSITION_BALANCE"));
    }

    @Test
    void downloadsACommodityDebitPdfThroughTheCompleteSpringContext() throws Exception {
        CommodityDebit entity = instantiateCommodityDebit();
        ReflectionTestUtils.setField(entity, "id", 990001L);
        ReflectionTestUtils.setField(entity, "transactionReference", "TRN-E2E");
        ReflectionTestUtils.setField(entity, "messageType", "606 Commodity Debit Advice");
        commodityDebitRepository.saveAndFlush(entity);

        byte[] body = mockMvc.perform(get("/api/v1/reports/commodity-debits/990001/pdf"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentType(APPLICATION_PDF))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string(CONTENT_DISPOSITION, "attachment; filename=\"community.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(body).startsWith("%PDF".getBytes());
    }

    @Test
    void returnsTheCommonJsonErrorWhenCommodityDebitDataIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/reports/commodity-debits/990099/pdf"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("COMMODITY_DEBIT_NOT_FOUND"))
                .andExpect(jsonPath("$.code").value("4011"))
                .andExpect(jsonPath("$.message")
                        .value("Commodity debit report data not found: 990099"))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void servesTableLabelsThroughTheCompleteSpringContext() throws Exception {
        mockMvc.perform(get("/api/v1/allTable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value("2000"))
                .andExpect(jsonPath("$.data.tableLabels.length()").value(3));
    }

    @Test
    void createsReadsUpdatesAndDeletesAHoliday() throws Exception {
        String createRequest = """
                {
                  "id": 880001,
                  "holidayDate": "2030-01-01",
                  "calendarCode": "SG",
                  "calendarName": "Integration Holiday",
                  "username": "integration-test"
                }
                """;

        mockMvc.perform(post("/api/v1/holidays")
                        .contentType(APPLICATION_JSON).content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value("2000"))
                .andExpect(jsonPath("$.data.id").value(880001));

        mockMvc.perform(get("/api/v1/holidays").param("pageNo", "0").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].id").value(880001))
                .andExpect(jsonPath("$.data.rows[0].calendarName").value("Integration Holiday"));

        String updateRequest = """
                {
                  "id": 880001,
                  "holidayDate": "2030-01-01",
                  "calendarCode": "SG",
                  "calendarName": "Updated Integration Holiday",
                  "username": "integration-test"
                }
                """;
        mockMvc.perform(put("/api/v1/holidays/880001")
                        .contentType(APPLICATION_JSON).content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(880001))
                .andExpect(jsonPath("$.data.calendarName").value("Updated Integration Holiday"));

        mockMvc.perform(delete("/api/v1/holidays/880001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(delete("/api/v1/holidays/880001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("HOLIDAY_NOT_FOUND"))
                .andExpect(jsonPath("$.code").value("4010"));
    }

    @Test
    void returnsTheCommonErrorEnvelopeForMalformedParameters() throws Exception {
        mockMvc.perform(get("/api/v1/POSITION_BALANCE").param("pageNo", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.code").value("4005"))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    private CommodityDebit instantiateCommodityDebit() throws Exception {
        var constructor = CommodityDebit.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
