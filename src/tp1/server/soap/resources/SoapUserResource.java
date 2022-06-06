package tp1.server.soap.resources;

import jakarta.jws.WebService;
import tp1.api.User;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.common.WebSupplier;
import tp1.common.exceptions.*;
import tp1.common.services.BasicUsersService;
import tp1.common.services.UsersService;
import tp1.server.soap.SoapUtils;

import java.util.List;
import java.util.logging.Logger;

/**
 * Soap class wrapping UsersService
 */
@WebService(serviceName= SoapUsers.NAME, targetNamespace=SoapUsers.NAMESPACE, endpointInterface=SoapUsers.INTERFACE)
public class SoapUserResource implements SoapUsers {
    private static Logger Log = Logger.getLogger(SoapUserResource.class.getName());

    private UsersService base = new BasicUsersService();

    @Override
    public String createUser(User user) throws UsersException {
        return handleExceptions(()->base.createUser(user));
    }

    @Override
    public User getUser(String userId, String password) throws UsersException {
        return handleExceptions(()->base.getUser(userId, password));
    }

    @Override
    public User updateUser(String userId, String password, User user) throws UsersException {
        return handleExceptions(()->base.updateUser(userId, password, user));
    }

    @Override
    public User deleteUser(String userId, String password) throws UsersException {
        return handleExceptions(()->base.deleteUser(userId, password));
    }

    @Override
    public List<User> searchUsers(String pattern) throws UsersException {
        return handleExceptions(()->base.searchUsers(pattern));
    }

    private static <T> T handleExceptions(WebSupplier<T> call) throws UsersException {
        try{
            return call.invoke();
        } catch (RequestTimeoutException | IncorrectPasswordException | InvalidArgumentException |
                 NoAccessException | InvalidUserIdException | UnexpectedErrorException |
                 InvalidFileLocationException | ConflicitingUsersException| InvalidTokenException e) {
            throw new UsersException(SoapUtils.logException(e, Log));
        }
    }
}
