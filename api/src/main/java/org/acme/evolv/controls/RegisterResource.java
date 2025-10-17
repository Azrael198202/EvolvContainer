package org.acme.evolv.controls;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.evolv.factory.services.*;
import java.util.Map;

@Path("/api/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RegisterResource {

    @Inject RegisterService service;

    @POST
    @Path("/register")
    @Transactional
    public Response register(Map<String, Object> payload) {
        try {
            Map<String, Object> result = service.register(payload);
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Registration failed: " + e.getMessage()).build();
        }
    }
}