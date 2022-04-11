package tp1.serverProxies;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tp1.api.service.rest.RestFiles;
import tp1.serverProxies.exceptions.RequestTimeoutException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

public class RestFilesServer implements FilesServerProxy{
    private static Logger Log = Logger.getLogger(RestFilesServer.class.getName());

    private WebTarget target;
    private String uri;

    public RestFilesServer(WebTarget target, String uri){
        this.target = target.path(RestFiles.PATH);
        this.uri = uri;
    }

    @Override
    public String getUri(){
        return uri;
    }

    @Override
    public void writeFile(String fileId, byte[] data, String token) throws RequestTimeoutException {
        Response r = target
                .path(fileId)
                .request()
                .post(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM));
    }

    @Override
    public void tryDeleteFile(String fileId, String token) {
        target.path(fileId).request().async().delete();
    }

    @Override
    public void redirectToGetFile(String fileId, String token) {
        Response r = Response.temporaryRedirect(target.path(fileId).getUri()).build();
        throw new WebApplicationException(r);
    }
}
