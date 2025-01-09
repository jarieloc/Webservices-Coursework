package org.acme.resources;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class ResourceService {
    private Map<String, Customer> customers = new HashMap<>();
    private Map<String, Merchant> merchants = new HashMap<>();

    public Map<String, Customer> getName() {
        return customers;
    }

    public Map<String, Merchant> getName() {
        return merchants;
    }
}