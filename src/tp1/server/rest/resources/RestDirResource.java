package tp1.server.rest.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import tp1.api.FileInfo;
import tp1.api.service.rest.RestDirectory;
import tp1.common.services.BasicDirectoryService;
import tp1.common.services.DirectoryService;

import java.util.*;
import java.util.logging.Logger;

import static tp1.server.rest.RestUtils.*;

/**
 * Rest class wrapping DirectoryService
 */
@Singleton
public class RestDirResource implements RestDirectory {
    private static final Logger Log = Logger.getLogger(RestDirResource.class.getName());
    private DirectoryService base = new BasicDirectoryService();

    @Override
    public FileInfo writeFile(String filename, byte[] data, String userId, String password) {
        return handleExceptions(()->base.writeFile(filename, data, userId, password), Log);
    }

    @Override
    public void deleteFile(String filename, String userId, String password) {
        handleExceptions(()->base.deleteFile(filename, userId, password), Log);
        throw new WebApplicationException(Response.Status.NO_CONTENT);
    }

    @Override
    public void shareFile(String filename, String userId, String userIdShare, String password) {
        handleExceptions(()->base.shareFile(filename, userId, userIdShare, password), Log);
        throw new WebApplicationException(Response.Status.NO_CONTENT);
    }

    @Override
    public void unshareFile(String filename, String userId, String userIdShare, String password) {
        handleExceptions(()->base.unshareFile(filename, userId, userIdShare, password), Log);
        throw new WebApplicationException(Response.Status.NO_CONTENT);
    }

    @Override
    public byte[] getFile(String filename, String userId, String accUserId, String password) {
        return handleExceptions(()->base.getFile(filename, userId, accUserId, password, true), Log);
    }

    @Override
    public List<FileInfo> lsFile(String userId, String password) {
        return handleExceptions(()->base.lsFile(userId, password), Log);
    }

    @Override
    public void deleteDirectory(String userId, String password) {
        handleExceptions(() -> base.deleteDirectory(userId, password), Log);
        throw new WebApplicationException(Response.Status.NO_CONTENT);
    }
}
