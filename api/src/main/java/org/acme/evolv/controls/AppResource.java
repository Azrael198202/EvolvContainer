package org.acme.evolv.controls;

import org.acme.evolv.dto.ChatMessage;
import org.acme.evolv.dto.CreateReq;
import org.acme.evolv.dto.CreateResp;
import org.acme.evolv.factory.services.VueFactoryService;
import org.acme.evolv.factory.shell.Shell;
import org.acme.evolv.utils.LogSseHub;
import org.acme.evolv.utils.PortUtils;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Path("/api/apps")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AppResource {

    @Inject
    VueFactoryService svc;

    @Inject
    LogSseHub hub; // for streaming logs sse/ws

    // -------------------- create (from scratch) --------------------
    @POST
    public CreateResp create(CreateReq req) throws Exception {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new BadRequestException("name required");
        }
        int p = (req.port() != null) ? req.port() : PortUtils.pickPort(req.name());

        var result = svc.createAndRun(req.companyId(), req.name(), p);

        return new CreateResp(
                req.companyId(),
                req.name(),
                p, 
                result.container(),
                result.image(),
                result.url(),
                result.logs());
    }

    // -------------------- create from template (with SSE logs) --------------------
    @POST
    @Path("/template/{name}")
    public VueFactoryService.Result createFromTemplate(            
            @PathParam("name") String name,
            @QueryParam("port") Integer port,
            @QueryParam("streamId") String streamId,   // front add streamId query param
            @QueryParam("companyId") String companyId
    ) throws Exception {
        int p = (port != null) ? port : PortUtils.pickPort(name);
        // important: pass streamId to svc 
        return svc.createFromTemplate(companyId, name, p, streamId);
    }

    // -------------------- remove --------------------
    @DELETE
    @Path("/{name}")
    public ChatMessage remove(@PathParam("name") String name) throws Exception { // add name param
        String safe = name.replaceAll("[^a-zA-Z0-9-_]", "-").toLowerCase();
        String container = "vue-" + safe;

        String dockerWinAbs = null; // Docker on Windows needs absolute path for -v
        String out = Shell.runDocker(
                List.of("rm", "-f", container),
                Duration.ofSeconds(15),
                /* ignoreNonZeroExit = */ true,
                dockerWinAbs
        );

        return new ChatMessage("removed: " + container + (out.isBlank() ? "" : (" | " + out.trim())));
    }

    // -------------------- health --------------------
    @GET
    @Path("/ping")
    public Map<String, Object> ping() {
        return Map.of("ok", true);
    }

    // -------------------- SSE stream --------------------
    @GET
    @Path("/streams/{id}")
    @Produces(jakarta.ws.rs.core.MediaType.SERVER_SENT_EVENTS)
    public void stream(@PathParam("id") String id,
                       @Context jakarta.ws.rs.sse.Sse sse,
                       @Context jakarta.ws.rs.sse.SseEventSink sink) {
        hub.register(id, sse, sink);
    }
}
