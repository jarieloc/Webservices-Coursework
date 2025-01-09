package org.acme.models;

public class Payment {
    private String id;
    private String customerId;  // ID of the customer making the payment
    private String merchantId;  // ID of the merchant receiving the payment
    private String customerName; // Name of the customer (optional for logging or display)
    private String merchantName; // Name of the merchant (optional for logging or display)
    private String customerCpr;  // CPR of the customer
    private String merchantCpr;  // CPR of the merchant
    private String customerBankAccount; // Bank account of the customer
    private String merchantBankAccount; // Bank account of the merchant
    private double amount; // Payment amount

    // Default constructor
    public Payment() {}

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getCustomerCpr() {
        return customerCpr;
    }

    public void setCustomerCpr(String customerCpr) {
        this.customerCpr = customerCpr;
    }

    public String getMerchantCpr() {
        return merchantCpr;
    }

    public void setMerchantCpr(String merchantCpr) {
        this.merchantCpr = merchantCpr;
    }

    public String getCustomerBankAccount() {
        return customerBankAccount;
    }

    public void setCustomerBankAccount(String customerBankAccount) {
        this.customerBankAccount = customerBankAccount;
    }

    public String getMerchantBankAccount() {
        return merchantBankAccount;
    }

    public void setMerchantBankAccount(String merchantBankAccount) {
        this.merchantBankAccount = merchantBankAccount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}

