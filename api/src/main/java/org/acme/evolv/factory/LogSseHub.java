package org.acme.evolv.factory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import jakarta.ws.rs.sse.SseBroadcaster;

import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class LogSseHub {
    private final ConcurrentHashMap<String, SseBroadcaster> broadcasters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Sse> sseRefs = new ConcurrentHashMap<>();

    public void register(String id, Sse sse, SseEventSink sink) {
        sseRefs.putIfAbsent(id, sse);
        broadcasters.compute(id, (k, oldB) -> {
            SseBroadcaster b = (oldB != null) ? oldB : sse.newBroadcaster();
            b.register(sink);
            return b;
        });
    }

    public void send(String id, String line) {
        Sse sse = sseRefs.get(id);
        SseBroadcaster b = broadcasters.get(id);
        if (sse != null && b != null) {
            b.broadcast(sse.newEventBuilder().data(line).build());
        }
    }

    public void close(String id) {
        SseBroadcaster b = broadcasters.remove(id);
        if (b != null) b.close();
        sseRefs.remove(id);
    }
}
