package tp1.serverProxies;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tp1.api.service.rest.RestFiles;
import tp1.serverProxies.exceptions.RequestTimeoutException;

import java.util.logging.Logger;
import static tp1.serverProxies.ClientUtils.reTry;

public class RestFilesClient implements FilesServerProxy{
    private static Logger Log = Logger.getLogger(RestFilesClient.class.getName());

    private WebTarget target;

    public RestFilesClient(String uri){
        this.target = ClientUtils.buildTarget(uri, RestFiles.PATH);
    }

    @Override
    public String getFileDirectUrl(String fileId) {
        return target.path(fileId).getUri().toString();
    }

    @Override
    public void writeFile(String fileId, byte[] data, String token) throws RequestTimeoutException {
        Response r = reTry(()-> target
                .path(fileId)
                .request()
                .post(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM)));
    }

    @Override
    public void deleteFileAsync(String fileId, String token) {
        ClientUtils.reTryAsync(
                ()-> target.path(fileId).request().delete(),
                (r) -> true
        );
    }

    @Override
    public void redirectToGetFile(String fileId, String token) {
        Response r = Response.temporaryRedirect(target.path(fileId).getUri()).build();
        throw new WebApplicationException(r);
    }
}
