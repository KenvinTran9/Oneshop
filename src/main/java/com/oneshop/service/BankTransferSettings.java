package com.oneshop.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Receiving account is configured by the shop; never accepted from checkout input. */
@Component
public class BankTransferSettings {
    private final String bankName, accountNumber, accountHolder, qrImage;
    public BankTransferSettings(@Value("${app.payments.bank-name:}") String bankName,
            @Value("${app.payments.account-number:}") String accountNumber,
            @Value("${app.payments.account-holder:}") String accountHolder,
            @Value("${app.payments.qr-image:}") String qrImage) {
        this.bankName = bankName.trim(); this.accountNumber = accountNumber.replaceAll("\\s", "");
        this.accountHolder = accountHolder.trim(); this.qrImage = qrImage.trim();
    }
    public boolean isConfigured() {
        return !bankName.isBlank() && bankName.length() <= 100 && accountNumber.matches("[A-Za-z0-9]{5,34}")
                && !accountHolder.isBlank() && accountHolder.length() <= 120;
    }
    public String getBankName() { return bankName; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolder() { return accountHolder; }
    public String getQrImage() {
        return qrImage.matches("/images/payments/[A-Za-z0-9._-]+\\.(png|jpg|jpeg|svg)") ? qrImage : null;
    }
}
