package org.acme.resources;

import org.acme.models.Merchant;
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
@Path("/merchants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MerchantResources {
    private Map<String, Merchant> merchants = new HashMap<>();

    // Initialize the BankService
    private final BankService bankService;

    public MerchantResources() {
        BankServiceService bankServiceService = new BankServiceService();
        this.bankService = bankServiceService.getBankServicePort();
    }

    @POST
    public Response createMerchant(Merchant merchant) {
        // Generate a unique ID if not provided
        if (merchant.getId() == null) {
            merchant.setId(UUID.randomUUID().toString());
        }

        // Only create a bank account if one isn't already set
        if (merchant.getBankAccount() == null) {
            try {
                User bankUser = new User();
                bankUser.setCprNumber(merchant.getCprNumber());
                bankUser.setFirstName(merchant.getFirstName());
                bankUser.setLastName(merchant.getLastName());

                String bankAccount = bankService.createAccountWithBalance(bankUser, new BigDecimal("1000"));
                merchant.setBankAccount(bankAccount);
            } catch (BankServiceException_Exception e) {
                if (e.getMessage().contains("Account already exists")) {
                    // If there's already an account, retrieve and set it
                    try {
                        String existingAccount = bankService.getAccountByCprNumber(merchant.getCprNumber()).getId();
                        merchant.setBankAccount(existingAccount);
                    } catch (BankServiceException_Exception ex2) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("Could not retrieve existing merchant account: " + ex2.getMessage())
                                .build();
                    }
                } else {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Failed to create merchant bank account: " + e.getMessage())
                            .build();
                }
            }
        }

        // Store the merchant in the map
        merchants.put(merchant.getId(), merchant);

        // Return successful response
        return Response.status(Response.Status.CREATED).entity(merchant).build();
    }

    /* @POST
    public Response createMerchant(Merchant merchant) {
        // Validate input
        if (merchant.getCprNumber() == null || merchant.getFirstName() == null || merchant.getLastName() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing merchant details").build();
        }

        // Generate a unique ID for the merchant
        if (merchant.getId() == null) {
            merchant.setId(UUID.randomUUID().toString());
        }

        // Create a bank account via BankService
        try {
            User bankUser = new User();
            bankUser.setCprNumber(merchant.getCprNumber());
            bankUser.setFirstName(merchant.getFirstName());
            bankUser.setLastName(merchant.getLastName());
            String bankAccount = bankService.createAccountWithBalance(bankUser, new BigDecimal("1000"));
            merchant.setBankAccount(bankAccount);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to create bank account: " + e.getMessage())
                    .build();
        }

        // Store the merchant in the map
        merchants.put(merchant.getId(), merchant);

        // Return a successful response
        return Response.status(Response.Status.CREATED).entity(merchant).build();
    } */

    @GET
    public Collection<Merchant> listMerchants() {
        return merchants.values();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteMerchant(@PathParam("id") String id) {
        Merchant merchant = merchants.remove(id);
        if (merchant == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Optionally remove the associated bank account
        try {
            if (merchant.getBankAccount() != null) {
                bankService.retireAccount(merchant.getBankAccount());
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to delete bank account: " + e.getMessage())
                    .build();
        }

        return Response.noContent().build();
    }

    public Merchant getMerchantById(String id) {
        return merchants.get(id);
    }

    @GET
    @Path("/{cprNumber}")
    public Response getMerchantByCpr(@PathParam("cprNumber") String cprNumber) {
        for (Merchant m : merchants.values()) {
            if (cprNumber.equals(m.getCprNumber())) {
                // Found it, return 200
                return Response.ok(m).build();
            }
        }
        // Not found
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}

