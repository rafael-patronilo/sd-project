package tp1.client.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tp1.api.service.rest.RestFiles;
import tp1.client.ClientUtils;
import tp1.common.clients.FilesServerClient;
import tp1.common.exceptions.InvalidFileLocationException;
import tp1.common.exceptions.RequestTimeoutException;

import java.util.logging.Logger;
import static tp1.client.ClientUtils.reTrySafe;

/**
 * Rest implementation for FilesServerClient
 */
public class RestFilesClient implements FilesServerClient {
    private static Logger Log = Logger.getLogger(RestFilesClient.class.getName());

    private WebTarget target;
    private String uri;

    public RestFilesClient(String uri) {
        this.uri = uri;
        this.target = ClientUtils.buildTarget(uri, RestFiles.PATH);
    }

    @Override
    public String getFileDirectUrl(String fileId) {
        return target.path(fileId).getUri().toString();
    }

    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public void writeFile(String fileId, byte[] data, String token, int maxRetries) throws RequestTimeoutException {
        Response r = ClientUtils.reTrySafe(()-> target
                .path(fileId)
                .request()
                .post(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM)), maxRetries);
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

    @Override
    public byte[] getFile(String fileId, String token) throws RequestTimeoutException, InvalidFileLocationException {
        Response r = reTrySafe(()-> target
                .path(fileId)
                .request()
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .get());
        return r.readEntity(byte[].class);
    }
}
