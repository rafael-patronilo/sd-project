package tp1.api.service.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tp1.common.services.DirectoryService;
import tp1.kafka.operations.Operation;

import java.util.List;

@Path(RestReplication.PATH)
public interface RestReplication {
    static final String PATH="/replication";
    /**
     * Notify this server of operations.
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    void notify(List<Operation> operations, @HeaderParam(DirectoryService.VERSION_HEADER) long version);

    /**
     *
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<Operation> getOps(@QueryParam("version") @DefaultValue("-1") long version);

}
