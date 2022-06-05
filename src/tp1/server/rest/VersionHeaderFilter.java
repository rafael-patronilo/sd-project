package tp1.server.rest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import tp1.common.services.DirectoryService;
import tp1.kafka.sync.SyncPoint;

import java.io.IOException;

@Provider
public class VersionHeaderFilter implements ContainerResponseFilter {
    private final SyncPoint<String> syncPoint = SyncPoint.getInstance();

    public VersionHeaderFilter(){}

    @Override
    public void filter(ContainerRequestContext containerRequestContext,
                       ContainerResponseContext containerResponseContext) throws IOException {
        containerResponseContext.getHeaders().add(DirectoryService.VERSION_HEADER, syncPoint.getVersion());
    }
}
