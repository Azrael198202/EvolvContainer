package org.acme.evolv.factory;

import org.acme.evolv.factory.shell.Shell;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Path("/api/apps")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AppResource {

    @Inject VueFactoryService svc;

    public record CreateReq(String name, Integer port) {}
    public record CreateResp(String name, int port, String container, String image, String url, String logs) {}
    public record Message(String message) {}

    @POST
    public CreateResp create(CreateReq req) throws Exception {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("name required");
        }
        int port = (req.port() != null) ? req.port() : VueFactoryService.pickPort(req.name());
        var r = svc.createAndRun(req.name(), port);
        return new CreateResp(req.name(), port, r.container(), r.image(), r.url(), r.logs());
    }

    @DELETE
    @Path("/{name}")
    public Message remove(@PathParam("name") String name) throws Exception {
        String safe = name.replaceAll("[^a-zA-Z0-9-_]", "-").toLowerCase();
        String container = "vue-" + safe;
        Shell.run(List.of("bash","-lc","docker rm -f " + container + " || true"), null, Duration.ofSeconds(15));
        return new Message("removed: " + container);
    }

    @GET @Path("/ping") public Map<String, Object> ping(){ return Map.of("ok", true); }
}
