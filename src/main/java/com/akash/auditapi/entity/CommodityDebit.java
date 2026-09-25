package com.akash.auditapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only report data mapped from {@code PMC_COMMODITY_DEBIT}.
 * Its fields mirror the Oracle columns used by the MT606 commodity debit PDF.
 */
@Entity
@Table(name = "PMC_COMMODITY_DEBIT")
public class CommodityDebit {
    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "INCOMING_REFERENCE")
    private String incomingReference;

    @Column(name = "TRANSACTION_REFERENCE")
    private String transactionReference;

    @Column(name = "MESSAGE_TYPE")
    private String messageType;

    @Column(name = "SENDER_BIC")
    private String senderBic;

    @Column(name = "SENDER_NAME")
    private String senderName;

    @Column(name = "SENDER_LOCATION")
    private String senderLocation;

    @Column(name = "RECEIVER_BIC")
    private String receiverBic;

    @Column(name = "RECEIVER_NAME")
    private String receiverName;

    @Column(name = "RECEIVER_LOCATION")
    private String receiverLocation;

    @Column(name = "NETWORK_CHANNEL")
    private String networkChannel;

    @Column(name = "NETWORK_REFERENCE")
    private String networkReference;

    @Column(name = "DATA_OWNER")
    private String dataOwner;

    @Column(name = "PHASE_ACTION")
    private String phaseAction;

    @Column(name = "MUR")
    private String mur;

    @Column(name = "RELATED_REFERENCE")
    private String relatedReference;

    @Column(name = "DELIVERY_LOCATION")
    private String deliveryLocation;

    @Column(name = "ALLOCATION")
    private String allocation;

    @Column(name = "COMMODITY_TYPE")
    private String commodityType;

    @Column(name = "ACCOUNT_IDENTIFICATION")
    private String accountIdentification;

    @Column(name = "VALUE_DATE")
    private LocalDate valueDate;

    @Column(name = "COMMODITY_UNIT")
    private String commodityUnit;

    @Column(name = "COMMODITY_AMOUNT")
    private BigDecimal commodityAmount;

    @Column(name = "COMMODITY_RECEIVER_IDENTIFIER")
    private String commodityReceiverIdentifier;

    @Column(name = "COMMODITY_RECEIVER_NAME")
    private String commodityReceiverName;

    @Column(name = "COMMODITY_RECEIVER_ADDRESS")
    private String commodityReceiverAddress;

    @Column(name = "BENEFICIARY_NAME")
    private String beneficiaryName;

    @Column(name = "BENEFICIARY_ADDRESS")
    private String beneficiaryAddress;

    @Column(name = "PRINTED_ON")
    private LocalDateTime printedOn;

    protected CommodityDebit() {
    }

    public Long getId() { return id; }
    public String getIncomingReference() { return incomingReference; }
    public String getTransactionReference() { return transactionReference; }
    public String getMessageType() { return messageType; }
    public String getSenderBic() { return senderBic; }
    public String getSenderName() { return senderName; }
    public String getSenderLocation() { return senderLocation; }
    public String getReceiverBic() { return receiverBic; }
    public String getReceiverName() { return receiverName; }
    public String getReceiverLocation() { return receiverLocation; }
    public String getNetworkChannel() { return networkChannel; }
    public String getNetworkReference() { return networkReference; }
    public String getDataOwner() { return dataOwner; }
    public String getPhaseAction() { return phaseAction; }
    public String getMur() { return mur; }
    public String getRelatedReference() { return relatedReference; }
    public String getDeliveryLocation() { return deliveryLocation; }
    public String getAllocation() { return allocation; }
    public String getCommodityType() { return commodityType; }
    public String getAccountIdentification() { return accountIdentification; }
    public LocalDate getValueDate() { return valueDate; }
    public String getCommodityUnit() { return commodityUnit; }
    public BigDecimal getCommodityAmount() { return commodityAmount; }
    public String getCommodityReceiverIdentifier() { return commodityReceiverIdentifier; }
    public String getCommodityReceiverName() { return commodityReceiverName; }
    public String getCommodityReceiverAddress() { return commodityReceiverAddress; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public String getBeneficiaryAddress() { return beneficiaryAddress; }
    public LocalDateTime getPrintedOn() { return printedOn; }
}
