package tp1.server.soap.resources;

import jakarta.jws.WebService;
import tp1.api.service.soap.FilesException;
import tp1.api.service.soap.SoapFiles;
import tp1.common.WebRunnable;
import tp1.common.WebSupplier;
import tp1.common.exceptions.*;
import tp1.common.services.FilesService;
import tp1.server.soap.SoapUtils;

import java.util.logging.Logger;

import static tp1.server.rest.RestUtils.handleExceptions;

@WebService(serviceName= SoapFiles.NAME, targetNamespace=SoapFiles.NAMESPACE, endpointInterface=SoapFiles.INTERFACE)
public class SoapFilesResource implements SoapFiles {
    private FilesService base = new FilesService();
    private static Logger Log = Logger.getLogger(SoapFilesResource.class.getName());

    @Override
    public byte[] getFile(String fileId, String token) throws FilesException {
        return handleExceptions(()->base.getFile(fileId, token));
    }

    @Override
    public void deleteFile(String fileId, String token) throws FilesException {
        handleExceptions(()->base.deleteFile(fileId, token));
    }

    @Override
    public void writeFile(String fileId, byte[] data, String token) throws FilesException {
        handleExceptions(()->base.writeFile(fileId, data, token));
    }

    private static void handleExceptions(WebRunnable call) throws FilesException {
        handleExceptions(()->{
            call.invoke();
            return null;
        });
    }

    private static <T> T handleExceptions(WebSupplier<T> call) throws FilesException {
        try{
            return call.invoke();
        } catch (RequestTimeoutException | IncorrectPasswordException | InvalidArgumentException |
                 NoAccessException | InvalidUserIdException | UnexpectedErrorException |
                 InvalidFileLocationException | ConflicitingUsersException e) {
            throw new FilesException(SoapUtils.logException(e, Log));
        }
    }
}
