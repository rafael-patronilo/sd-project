package tp1.server.soap.resources;

import jakarta.jws.WebService;
import tp1.api.FileInfo;
import tp1.api.service.soap.DirectoryException;
import tp1.api.service.soap.SoapDirectory;
import tp1.common.WebRunnable;
import tp1.common.WebSupplier;
import tp1.common.exceptions.*;
import tp1.common.services.BasicDirectoryService;
import tp1.common.services.DirectoryService;
import tp1.server.soap.SoapUtils;

import java.util.List;
import java.util.logging.Logger;

/**
 * Soap class wrapping DirectoryService
 */
@WebService(serviceName= SoapDirectory.NAME, targetNamespace=SoapDirectory.NAMESPACE, endpointInterface=SoapDirectory.INTERFACE)
public class SoapDirResource implements SoapDirectory {

    private DirectoryService service = new BasicDirectoryService();

    private static Logger Log = Logger.getLogger(SoapDirResource.class.getName());

    @Override
    public FileInfo writeFile(String filename, byte[] data, String userId, String password) throws DirectoryException {
        return handleExceptions(() -> service.writeFile(filename, data, userId, password));
    }

    @Override
    public void deleteFile(String filename, String userId, String password) throws DirectoryException {
        handleExceptions(() -> service.deleteFile(filename, userId, password));
    }

    @Override
    public void deleteDirectory(String userId, String password, String token) throws DirectoryException {
        handleExceptions(() -> service.deleteDirectory(userId, password, token));
    }

    @Override
    public void shareFile(String filename, String userId, String userIdShare, String password) throws DirectoryException {
        handleExceptions(() -> service.shareFile(filename, userId, userIdShare, password));
    }

    @Override
    public void unshareFile(String filename, String userId, String userIdShare, String password) throws DirectoryException {
        handleExceptions(() -> service.unshareFile(filename, userId, userIdShare, password));
    }

    @Override
    public byte[] getFile(String filename, String userId, String accUserId, String password) throws DirectoryException {
        return handleExceptions(() -> service.getFile(filename, userId, accUserId, password, false, -1L));
    }

    @Override
    public List<FileInfo> lsFile(String userId, String password) throws DirectoryException {
        return handleExceptions(() -> service.lsFile(userId, password));
    }

    private static void handleExceptions(WebRunnable call) throws DirectoryException {
        handleExceptions(()->{
            call.invoke();
            return null;
        });
    }
    private static <T> T handleExceptions(WebSupplier<T> call) throws DirectoryException {
        try{
            return call.invoke();
        } catch (RequestTimeoutException | IncorrectPasswordException | InvalidArgumentException |
                 NoAccessException | InvalidUserIdException | UnexpectedErrorException |
                 InvalidFileLocationException | ConflicitingUsersException | InvalidTokenException e) {
            throw new DirectoryException(SoapUtils.logException(e, Log));
        }
    }

}
