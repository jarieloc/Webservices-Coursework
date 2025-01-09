package org.acme.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.acme.models.Payment;

import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceService;

import org.acme.models.Customer;
import org.acme.models.Merchant;

@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResources {

    private List<Payment> payments = new ArrayList<>();

    @Inject
    private CustomerResources customerResources;

    @Inject
    private MerchantResources merchantResources;

    @Inject
    private final BankService bankService;

    public PaymentResources() {
        BankServiceService bankServiceService = new BankServiceService();
        this.bankService = bankServiceService.getBankServicePort();
    }

    @POST
    public Response makePayment(Payment payment) {
        // Retrieve customer and merchant from in-memory maps
        Customer customer = customerResources.getCustomerById(payment.getCustomerId());
        Merchant merchant = merchantResources.getMerchantById(payment.getMerchantId());

        if (customer == null || merchant == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("Customer or Merchant not found")
                .build();
        }

        // Populate payment details (optional, for recordkeeping)
        payment.setCustomerName(customer.getFirstName() + " " + customer.getLastName());
        payment.setMerchantName(merchant.getFirstName() + " " + merchant.getLastName());
        payment.setCustomerCpr(customer.getCprNumber());
        payment.setMerchantCpr(merchant.getCprNumber());
        payment.setCustomerBankAccount(customer.getBankAccount());
        payment.setMerchantBankAccount(merchant.getBankAccount());

        // **New: Actually transfer the money in the SOAP bank**
        try {
            BigDecimal transferAmount = BigDecimal.valueOf(payment.getAmount());
            System.out.println("Transferring amount: " + transferAmount + " kr");
    
            bankService.transferMoneyFromTo(
                customer.getBankAccount(),
                merchant.getBankAccount(),
                transferAmount,
                "Payment from DTU Pay"
            );
    
            System.out.println("Transfer successful: " + transferAmount + " kr from Customer " + customer.getId() + " to Merchant " + merchant.getId());
        } catch (Exception e) {
            System.err.println("Transfer failed: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Bank transfer failed: " + e.getMessage())
                .build();
        }

        // If success, add the payment record to our in-memory list (if you want to keep it)
        payments.add(payment);

        // Return 201 Created with the payment details
        return Response.status(Response.Status.CREATED).entity(payment).build();
    }

    // @POST
    // public Response makePayment(Payment payment) {
    //     // Retrieve customer and merchant
    //     Customer customer = customerResources.getCustomerById(payment.getCustomerId());
    //     Merchant merchant = merchantResources.getMerchantById(payment.getMerchantId());

    //     if (customer == null || merchant == null) {
    //         return Response.status(Response.Status.NOT_FOUND)
    //             .entity("Customer or Merchant not found").build();
    //     }

    //     // Populate payment details
    //     payment.setCustomerName(customer.getFirstName() + " " + customer.getLastName());
    //     payment.setMerchantName(merchant.getFirstName() + " " + merchant.getLastName());
    //     payment.setCustomerCpr(customer.getCprNumber());
    //     payment.setMerchantCpr(merchant.getCprNumber());
    //     payment.setCustomerBankAccount(customer.getBankAccount());
    //     payment.setMerchantBankAccount(merchant.getBankAccount());

    //     // Add payment to the list
    //     payments.add(payment);

    //     return Response.status(Response.Status.CREATED).entity(payment).build();
    // }

    @GET
    public List<Payment> listPayments() {
        return payments;
    }
}