package com.akash.auditapi.dto.response;

import com.akash.auditapi.entity.CommodityDebit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Immutable database-backed data consumed by the commodity debit PDF template.
 * It keeps persistence concerns out of the reusable document renderer.
 */
public record CommodityDebitReportDto(
        Long id,
        String incomingReference,
        String transactionReference,
        String messageType,
        String senderBic,
        String senderName,
        String senderLocation,
        String receiverBic,
        String receiverName,
        String receiverLocation,
        String networkChannel,
        String networkReference,
        String dataOwner,
        String phaseAction,
        String mur,
        String relatedReference,
        String deliveryLocation,
        String allocation,
        String commodityType,
        String accountIdentification,
        LocalDate valueDate,
        String commodityUnit,
        BigDecimal commodityAmount,
        String commodityReceiverIdentifier,
        String commodityReceiverName,
        String commodityReceiverAddress,
        String beneficiaryName,
        String beneficiaryAddress,
        LocalDateTime printedOn) {

    public static CommodityDebitReportDto from(CommodityDebit entity) {
        return new CommodityDebitReportDto(
                entity.getId(), entity.getIncomingReference(), entity.getTransactionReference(),
                entity.getMessageType(), entity.getSenderBic(), entity.getSenderName(),
                entity.getSenderLocation(), entity.getReceiverBic(), entity.getReceiverName(),
                entity.getReceiverLocation(), entity.getNetworkChannel(), entity.getNetworkReference(),
                entity.getDataOwner(), entity.getPhaseAction(), entity.getMur(),
                entity.getRelatedReference(), entity.getDeliveryLocation(), entity.getAllocation(),
                entity.getCommodityType(), entity.getAccountIdentification(), entity.getValueDate(),
                entity.getCommodityUnit(), entity.getCommodityAmount(),
                entity.getCommodityReceiverIdentifier(), entity.getCommodityReceiverName(),
                entity.getCommodityReceiverAddress(), entity.getBeneficiaryName(),
                entity.getBeneficiaryAddress(), entity.getPrintedOn());
    }
}
