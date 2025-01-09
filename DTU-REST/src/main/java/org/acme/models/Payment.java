package org.acme.models;

public class Payment {
    private String id;
    private String customerId;
    private String merchantId;
    private String customerName;
    private String merchantName;
    private double amount;

    // Default constructor
    public Payment(){}


    // Getters and Setters
    public String getCustomerId(){
        return customerId;
    }

    public void setCustomerId(String customerId){
        this.customerId = customerId;
    }

    public String getMerchantId(){
        return merchantId;
    }

    public void setMerchantId(String merchantId){
        this.merchantId = merchantId;
    }

    public double getAmount(){
        return amount;
    }

    public void setAmount(int amount){
        this.amount = amount;
    }
}
