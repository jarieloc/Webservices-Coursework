package org.acme.resources;

import org.acme.models.Customer;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.models.Payment;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.UUID;

@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class CustomerResources {
    private Map<String, Customer> customers = new HashMap<>();

    @POST
    public Response createCustomer(Customer customer){
        if (customer.getId() == null) {
            customer.setId(UUID.randomUUID().toString()); // Generate an ID if missing
        }
        customer.setId(UUID.randomUUID().toString());
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
