package tp1.server.rest.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.service.rest.RestFiles;
import tp1.common.services.FilesService;

import java.util.logging.Logger;
import static tp1.server.rest.RestUtils.*;
@Singleton
public class RestFilesResource implements RestFiles {
    private static Logger Log = Logger.getLogger(RestFilesResource.class.getName());
    private FilesService base = new FilesService();

    @Override
    public void writeFile(String fileId, byte[] data, String token) {
        handleExceptions(()->base.writeFile(fileId, data, token), Log);
        throw new WebApplicationException(Status.NO_CONTENT);
    }

    @Override
    public void deleteFile(String fileId, String token) {
        handleExceptions(()->base.deleteFile(fileId, token), Log);
        throw new WebApplicationException(Status.NO_CONTENT);
    }

    @Override
    public byte[] getFile(String fileId, String token) {
        return handleExceptions(()->base.getFile(fileId, token), Log);
    }

}
