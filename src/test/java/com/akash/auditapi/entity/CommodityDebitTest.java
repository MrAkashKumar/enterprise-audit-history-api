package com.akash.auditapi.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CommodityDebitTest {
    @Test
    void exposesEveryMappedReportColumn() {
        CommodityDebit entity = new CommodityDebit();
        LocalDate valueDate = LocalDate.of(2026, 7, 3);
        LocalDateTime printedOn = LocalDateTime.of(2026, 7, 3, 15, 18, 47);
        BigDecimal amount = new BigDecimal("2770");
        Object[][] values = {
                {"id", 10L}, {"incomingReference", "REF-10"},
                {"transactionReference", "TRN-10"}, {"messageType", "606 Commodity Debit Advice"},
                {"senderBic", "SENDERBIC"}, {"senderName", "Sender Bank"},
                {"senderLocation", "London"}, {"receiverBic", "RECEIVERBIC"},
                {"receiverName", "Receiver Bank"}, {"receiverLocation", "Singapore"},
                {"networkChannel", "CHANNEL"}, {"networkReference", "NETWORK-10"},
                {"dataOwner", "OWNER"}, {"phaseAction", "Processing"}, {"mur", "MUR-10"},
                {"relatedReference", "RELATED-10"}, {"deliveryLocation", "LONDON"},
                {"allocation", "UNALL"}, {"commodityType", "GOLD"},
                {"accountIdentification", "ACCOUNT-10"}, {"valueDate", valueDate},
                {"commodityUnit", "FOZ"}, {"commodityAmount", amount},
                {"commodityReceiverIdentifier", "PARTYBIC"},
                {"commodityReceiverName", "Party Name"},
                {"commodityReceiverAddress", "Party Address"},
                {"beneficiaryName", "Beneficiary"}, {"beneficiaryAddress", "Beneficiary Address"},
                {"printedOn", printedOn}
        };
        for (Object[] value : values) {
            ReflectionTestUtils.setField(entity, (String) value[0], value[1]);
        }

        assertThat(entity.getId()).isEqualTo(10L);
        assertThat(entity.getIncomingReference()).isEqualTo("REF-10");
        assertThat(entity.getTransactionReference()).isEqualTo("TRN-10");
        assertThat(entity.getMessageType()).isEqualTo("606 Commodity Debit Advice");
        assertThat(entity.getSenderBic()).isEqualTo("SENDERBIC");
        assertThat(entity.getSenderName()).isEqualTo("Sender Bank");
        assertThat(entity.getSenderLocation()).isEqualTo("London");
        assertThat(entity.getReceiverBic()).isEqualTo("RECEIVERBIC");
        assertThat(entity.getReceiverName()).isEqualTo("Receiver Bank");
        assertThat(entity.getReceiverLocation()).isEqualTo("Singapore");
        assertThat(entity.getNetworkChannel()).isEqualTo("CHANNEL");
        assertThat(entity.getNetworkReference()).isEqualTo("NETWORK-10");
        assertThat(entity.getDataOwner()).isEqualTo("OWNER");
        assertThat(entity.getPhaseAction()).isEqualTo("Processing");
        assertThat(entity.getMur()).isEqualTo("MUR-10");
        assertThat(entity.getRelatedReference()).isEqualTo("RELATED-10");
        assertThat(entity.getDeliveryLocation()).isEqualTo("LONDON");
        assertThat(entity.getAllocation()).isEqualTo("UNALL");
        assertThat(entity.getCommodityType()).isEqualTo("GOLD");
        assertThat(entity.getAccountIdentification()).isEqualTo("ACCOUNT-10");
        assertThat(entity.getValueDate()).isEqualTo(valueDate);
        assertThat(entity.getCommodityUnit()).isEqualTo("FOZ");
        assertThat(entity.getCommodityAmount()).isEqualByComparingTo(amount);
        assertThat(entity.getCommodityReceiverIdentifier()).isEqualTo("PARTYBIC");
        assertThat(entity.getCommodityReceiverName()).isEqualTo("Party Name");
        assertThat(entity.getCommodityReceiverAddress()).isEqualTo("Party Address");
        assertThat(entity.getBeneficiaryName()).isEqualTo("Beneficiary");
        assertThat(entity.getBeneficiaryAddress()).isEqualTo("Beneficiary Address");
        assertThat(entity.getPrintedOn()).isEqualTo(printedOn);
    }
}
