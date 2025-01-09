package org.acme.models;

public class Customer {
    private String id;
    private String name;

    // Default constructor
    public Customer() {}

    // Getters and Setters
    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }
}
