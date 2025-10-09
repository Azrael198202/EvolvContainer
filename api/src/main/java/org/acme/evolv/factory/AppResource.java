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
        int p = (req.port != null) ? req.port : PortUtils.pickPort(req.name());
        var result = svc.createAndRun(req.name(), p);

        return new CreateResp(
                req.name(),
                req.port,
                result.container(),
                result.image(),
                result.url(),
                result.logs());
    }
    
    @POST
    @Path("/template/{name}")
    public VueFactoryService.Result createFromTemplate(
            @PathParam("name") String name,
            @QueryParam("port") Integer port
    ) throws Exception {
        int p = (port != null) ? port : PortUtils.pickPort(name);
        return svc.createFromTemplate(name, p);
    }

    @DELETE
    @Path("/{name}")
    public Message remove(String name) throws Exception {
        String safe = name.replaceAll("[^a-zA-Z0-9-_]", "-").toLowerCase();
        String container = "vue-" + safe;

        String dockerWinAbs = null; 

        // docker rm -f <container>
        String out = Shell.runDocker(
                List.of("rm", "-f", container),
                Duration.ofSeconds(15),
                /* ignoreNonZeroExit = */ true, 
                dockerWinAbs
        );

        return new Message("removed: " + container + (out.isBlank() ? "" : (" | " + out.trim())));
    }
    @GET @Path("/ping") public Map<String, Object> ping(){ return Map.of("ok", true); }
}
