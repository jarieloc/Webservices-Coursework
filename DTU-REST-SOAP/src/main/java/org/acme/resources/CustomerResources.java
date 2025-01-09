package org.acme.resources;

import org.acme.models.Customer;
/* import org.acme.models.Merchant; */

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceException_Exception;
import dtu.ws.fastmoney.BankServiceService;
import dtu.ws.fastmoney.User;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResources {

    private Map<String, Customer> customers = new HashMap<>();

    // Initialize the BankService
    private final BankService bankService;

    public CustomerResources() {
        BankServiceService bankServiceService = new BankServiceService();
        this.bankService = bankServiceService.getBankServicePort();
    }

    @POST
    public Response createCustomer(Customer customer) {
        if (customer.getId() == null) {
            customer.setId(UUID.randomUUID().toString());
        }

        // If the step definitions already set a bankAccount, you can skip creation.
        if (customer.getBankAccount() == null) {
            try {
                User bankUser = new User();
                bankUser.setCprNumber(customer.getCprNumber());
                bankUser.setFirstName(customer.getFirstName());
                bankUser.setLastName(customer.getLastName());
                String bankAccount = bankService.createAccountWithBalance(bankUser, new BigDecimal("1000"));
                customer.setBankAccount(bankAccount);
            } catch (BankServiceException_Exception e) {
                if (e.getMessage().contains("Account already exists")) {
                    // If there's already an account, retrieve it:
                    try {
                        String existingAccount = bankService.getAccountByCprNumber(customer.getCprNumber()).getId();
                        customer.setBankAccount(existingAccount);
                    } catch (BankServiceException_Exception ex2) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("Could not retrieve existing account: " + ex2.getMessage())
                                .build();
                    }
                } else {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Failed to create bank account: " + e.getMessage())
                            .build();
                }
            }
        }

        // Now store the customer in the map.
        customers.put(customer.getId(), customer);

        return Response.status(Response.Status.CREATED).entity(customer).build();
    }

    /* @POST
    public Response createCustomer(Customer customer) {
        // Validate input
        if (customer.getCprNumber() == null || customer.getFirstName() == null || customer.getLastName() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing customer details").build();
        }

        // Generate a unique ID for the customer
        if (customer.getId() == null) {
            customer.setId(UUID.randomUUID().toString());
        }

        // Create a bank account via BankService
        try {
            User bankUser = new User();
            bankUser.setCprNumber(customer.getCprNumber());
            bankUser.setFirstName(customer.getFirstName());
            bankUser.setLastName(customer.getLastName());
            String bankAccount = bankService.createAccountWithBalance(bankUser, new BigDecimal("1000"));
            customer.setBankAccount(bankAccount);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to create bank account: " + e.getMessage())
                    .build();
        }

        // Store the customer in the map
        customers.put(customer.getId(), customer);

        // Return a successful response
        return Response.status(Response.Status.CREATED).entity(customer).build();
    } */

    @GET
    public Collection<Customer> listCustomers() {
        return customers.values();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCustomer(@PathParam("id") String id) {
        Customer customer = customers.remove(id);
        if (customer == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    public Customer getCustomerById(String id) {
        return customers.get(id);
    }

    @GET
    @Path("/{cprNumber}")
    public Response getCustomerByCpr(@PathParam("cprNumber") String cprNumber) {
        // Find a customer by that CPR in the `customers` map
        // Remember: your map is keyed by `customer.getId()`, not CPR.
        // So you need to iterate through the map to find a matching CPR:
        for (Customer c : customers.values()) {
            if (cprNumber.equals(c.getCprNumber())) {
                // Found it, return HTTP 200
                return Response.ok(c).build();
            }
        }
        // If we get here, no customer found with that CPR
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}


/* package org.acme.resources;

import org.acme.models.Customer;
import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceService;
import dtu.ws.fastmoney.User;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DELETE;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.UUID;

@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResources {

    private Map<String, Customer> customers = new HashMap<>();

    @Inject
    BankService bankService;

    @POST
    public Response createCustomer(Customer customer) {
        if (customer.getCprNumber() == null || customer.getFirstName() == null || customer.getLastName() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing customer details").build();
        }

        if (customer.getId() == null) {
            customer.setId(UUID.randomUUID().toString()); // Generate an ID if missing
        }

        // Create bank account via BankService
        try {
            User bankUser = new User();
            bankUser.setCprNumber(customer.getCprNumber()); 
            bankUser.setFirstName(customer.getFirstName());
            bankUser.setLastName(customer.getLastName());
            String bankAccount = bankService.createAccountWithBalance(bankUser, 1000);
            customer.setBankAccount(bankAccount);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Failed to create bank account: " + e.getMessage())
                        .build();
        }

        customers.put(customer.getId(), customer);
        return Response.status(Response.Status.CREATED).entity(customer).build();
    } */


    /* @POST
    public Response createCustomer(Customer customer) {
        if (customer.getId() == null) {
            customer.setId(UUID.randomUUID().toString()); // Generate an ID if missing
        }

        // Create bank account via BankService
        try {
            User bankUser = new User(customer.getFirstName(), customer.getLastName(), customer.getCpr());
            String bankAccount = bankService.createAccountWithBalance(bankUser, 1000);
            customer.setBankAccount(bankAccount);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to create bank account: " + e.getMessage())
                    .build();
        }

        customers.put(customer.getId(), customer);
        return Response.status(Response.Status.CREATED).entity(customer).build();
    } */

 /*    @GET
    public Collection<Customer> listCustomers() {
        return customers.values();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCustomer(@PathParam("id") String id) {
        Customer customer = customers.remove(id);
        if (customer == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Optionally remove the associated bank account
        try {
            bankService.retireAccount(customer.getBankAccount());
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to delete bank account: " + e.getMessage())
                    .build();
        }

        return Response.noContent().build();
    }

    public Customer getCustomerById(String id) {
        return customers.get(id);
    }
} */



/* package org.acme.resources;

import org.acme.models.Customer;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DELETE;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.models.Payment;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceService;
import dtu.ws.fastmoney.User;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.UUID;

@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class CustomerResources {
    private Map<String, Customer> customers = new HashMap<>();


    @Inject
    BankService bankService;

    @POST
    public Response createCustomer(Customer customer){
        if (customer.getId() == null) {
            customer.setId(UUID.randomUUID().toString()); // Generate an ID if missing
        }
        customer.setId(UUID.randomUUID().toString());

        // Create bank account via BankService
        try {
            BankService bankService = bankServiceService.getBankServicePort();
            User bankUser = new User(customer.getFirstName(), customer.getLastName(), customer.getCpr());
            String bankAccount = bankService.createAccountWithBalance(bankUser, 1000);
            customer.setBankAccount(bankAccount);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Failed to create bank account: " + e.getMessage())
                           .build();
        }


        customers.put(customer.getId(), customer);
        return Response.status(Response.Status.CREATED).entity(customer).build();
    }

    @GET
    public Collection<Customer> listCustomers(){
        return customers.values();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCustomer(@PathParam("id") String id) {
        Customer customer = customers.remove(id);
        if (customer == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }



}
 */