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
import tp1.common.services.DirectoryService;
import tp1.tokens.TemporaryToken;
import tp1.tokens.TokenManager;

import java.util.logging.Logger;
import static tp1.client.ClientUtils.reTrySafe;

/**
 * Rest implementation for FilesServerClient
 */
public class RestFilesClient implements FilesServerClient {
    private final String permanentToken = TokenManager.serializeToken(TokenManager.createPermanentToken());
    private static Logger Log = Logger.getLogger(RestFilesClient.class.getName());

    private WebTarget target;
    private String uri;

    public RestFilesClient(String uri) {
        this.uri = uri;
        this.target = ClientUtils.buildTarget(uri, RestFiles.PATH);
    }

    /**
     * Retrieves or generates a new access token for a given file
     * @param fileId the file to access
     * @return the token
     */
    private TemporaryToken tokenFor(String fileId){
        TemporaryToken token = TokenManager.createTemporaryToken(fileId);
        String encoded = TokenManager.serializeToken(token);
        Log.info("Created temporary token:" +
                " timestamp = " + token.getTimestamp()
                + "; fileId = " + token.getFileId()
                + "; hash = " + token.getHash()
                + "\n encoded = " + encoded);
        return token;
    }


    private WebTarget fileTargetWithTempToken(String fileId){
        TemporaryToken token = tokenFor(fileId);
        String encoded = TokenManager.serializeToken(token);
        return fileTarget(fileId, encoded);
    }

    /**
     * Builds the path to file and adds a temporary token
     * @param fileId the file's id
     * @return the web target with the required path
     */
    private WebTarget fileTarget(String fileId, String token){
        return fileTarget(fileId).queryParam("token", token);
    }

    private WebTarget fileTarget(String fileId){
        return target.path(fileId);
    }

    @Override
    public String getFileDirectUrl(String fileId) {
        return fileTarget(fileId).getUri().toString();
    }

    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public void writeFile(String fileId, byte[] data, int maxRetries) throws RequestTimeoutException {
        Response r = ClientUtils.reTrySafe(()-> fileTarget(fileId, permanentToken)
                .request()
                .post(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM)), maxRetries);
    }

    @Override
    public void deleteFileAsync(String fileId) {
        ClientUtils.reTryAsync(
                ()-> fileTarget(fileId, permanentToken).request().delete(),
                (r) -> true
        );
    }

    @Override
    public void redirectToGetFile(String fileId) {
        redirectToGetFile(fileId, -1L);
    }

    @Override
    public void redirectToGetFile(String fileId, long version) {
        Response r = Response.temporaryRedirect(fileTargetWithTempToken(fileId).getUri())
                .header(DirectoryService.VERSION_HEADER, version).build();
        throw new WebApplicationException(r);
    }

    @Override
    public byte[] getFile(String fileId) throws RequestTimeoutException, InvalidFileLocationException {
        return getFile(fileId, -1L);
    }


    @Override
    public byte[] getFile(String fileId, long version) throws RequestTimeoutException, InvalidFileLocationException {
        Response r = reTrySafe(()-> target
                .path(fileId)
                .queryParam("token", permanentToken)
                .request()
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .header(DirectoryService.VERSION_HEADER, version)
                .get());
        return r.readEntity(byte[].class);
    }
}
