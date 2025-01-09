package org.acme.models;

public class Customer {
    private String id;
    private String firstName;
    private String lastName;
    private String cprNumber;
    private String bankAccount;

    /* private String name; */

    // Default constructor
    public Customer() {}

    // Getters and Setters
    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getFirstName(){
        return firstName;
    }

    public void setFirstName(String firstName){
        this.firstName = firstName;
    }

    public String getLastName(){
        return lastName;
    }

    public void setLastName(String lastName){
        this.lastName = lastName;
    }

    public String getCprNumber() {
        return cprNumber;
    }
    
    public void setCprNumber(String cprNumber) {
        this.cprNumber = cprNumber;
    }

    /* public String getCpr(){
        return cpr;
    }

    public void setCpr(String cpr){
        this.cpr = cpr;
    } */

    public String getBankAccount(){
        return bankAccount;
    }

    public void setBankAccount(String bankAccount){
        this.bankAccount = bankAccount;
    }

}
