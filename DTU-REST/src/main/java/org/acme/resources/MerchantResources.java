package org.acme.resources;

import org.acme.models.Merchant;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.models.Customer;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.UUID;

@Path("/merchants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class MerchantResources {
    private Map<String, Merchant> merchants = new HashMap<>();

    @POST
    public Response createMerchant(Merchant merchant){
        if (merchant.getId() == null) {
            merchant.setId(UUID.randomUUID().toString()); // Generate an ID if missing
        }
        merchant.setId(UUID.randomUUID().toString());
        merchants.put(merchant.getId(), merchant);
        return Response.status(Response.Status.CREATED).entity(merchant).build();
    }

    @GET
    public Collection<Merchant> listMerchants(){
        return merchants.values();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteMerchant(@PathParam("id") String id) {
        Merchant merchant = merchants.remove(id);
        if (merchant == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

}
