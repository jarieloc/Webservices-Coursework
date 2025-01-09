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
import java.util.ArrayList;

import org.acme.models.Payment;

/* 
import org.acme.resources.ResourceService; */

@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class PaymentResources {
    private List<Payment> payments = new ArrayList<>();
/* 
    @Inject
    ResourceService resourceService; */

    /* @POST
    public Response makePayment(Payment payment){
        if (!resourceService.getCustomers().containsKey(payment.getCustomerId()) ||
            !resourceService.getMerchants().containsKey(payment.getMerchantId())) {
            return Response.status(Response.Status.NOT_FOUND).entity("Customer or Merchant not found").build();
        }

        // Set additional information for the payment
        payment.setCustomerName(resourceService.getCustomers().get(payment.getCustomerId()).getName());
        payment.setMerchantName(resourceService.getMerchants().get(payment.getMerchantId()).getName());



        payments.add(payment);
        return Response.status(Response.Status.CREATED).entity(payment).build();
    } */

    @POST
    public Response makePayment(Payment payment) {
        payments.add(payment);
        return Response.status(Response.Status.CREATED).entity(payment).build();
    }

    @GET
    public List<Payment> listPayments(){
        return payments;
    }

}
