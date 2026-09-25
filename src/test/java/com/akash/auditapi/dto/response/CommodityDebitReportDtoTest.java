package com.akash.auditapi.dto.response;

import com.akash.auditapi.entity.CommodityDebit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommodityDebitReportDtoTest {
    @Test
    void mapsEveryDatabaseValueFromTheEntity() {
        CommodityDebit entity = mock(CommodityDebit.class);
        LocalDate valueDate = LocalDate.of(2026, 7, 3);
        LocalDateTime printedOn = LocalDateTime.of(2026, 7, 3, 15, 18, 47);
        BigDecimal amount = new BigDecimal("2770");
        when(entity.getId()).thenReturn(10L);
        when(entity.getIncomingReference()).thenReturn("REF");
        when(entity.getTransactionReference()).thenReturn("TRN");
        when(entity.getMessageType()).thenReturn("MT606");
        when(entity.getSenderBic()).thenReturn("SBIC");
        when(entity.getSenderName()).thenReturn("SNAME");
        when(entity.getSenderLocation()).thenReturn("SLOC");
        when(entity.getReceiverBic()).thenReturn("RBIC");
        when(entity.getReceiverName()).thenReturn("RNAME");
        when(entity.getReceiverLocation()).thenReturn("RLOC");
        when(entity.getNetworkChannel()).thenReturn("CHANNEL");
        when(entity.getNetworkReference()).thenReturn("NREF");
        when(entity.getDataOwner()).thenReturn("OWNER");
        when(entity.getPhaseAction()).thenReturn("ACTION");
        when(entity.getMur()).thenReturn("MUR");
        when(entity.getRelatedReference()).thenReturn("RELATED");
        when(entity.getDeliveryLocation()).thenReturn("DELIVERY");
        when(entity.getAllocation()).thenReturn("ALLOCATION");
        when(entity.getCommodityType()).thenReturn("GOLD");
        when(entity.getAccountIdentification()).thenReturn("ACCOUNT");
        when(entity.getValueDate()).thenReturn(valueDate);
        when(entity.getCommodityUnit()).thenReturn("FOZ");
        when(entity.getCommodityAmount()).thenReturn(amount);
        when(entity.getCommodityReceiverIdentifier()).thenReturn("PARTY-ID");
        when(entity.getCommodityReceiverName()).thenReturn("PARTY");
        when(entity.getCommodityReceiverAddress()).thenReturn("PARTY-ADDRESS");
        when(entity.getBeneficiaryName()).thenReturn("BENEFICIARY");
        when(entity.getBeneficiaryAddress()).thenReturn("BENEFICIARY-ADDRESS");
        when(entity.getPrintedOn()).thenReturn(printedOn);

        CommodityDebitReportDto dto = CommodityDebitReportDto.from(entity);

        assertThat(dto).isEqualTo(new CommodityDebitReportDto(
                10L, "REF", "TRN", "MT606", "SBIC", "SNAME", "SLOC",
                "RBIC", "RNAME", "RLOC", "CHANNEL", "NREF", "OWNER", "ACTION",
                "MUR", "RELATED", "DELIVERY", "ALLOCATION", "GOLD", "ACCOUNT",
                valueDate, "FOZ", amount, "PARTY-ID", "PARTY", "PARTY-ADDRESS",
                "BENEFICIARY", "BENEFICIARY-ADDRESS", printedOn));
    }
}
