package tp1.client.soap;

import tp1.api.User;
import tp1.api.service.soap.SoapUsers;
import tp1.client.ClientUtils;
import tp1.common.clients.UsersServerClient;
import tp1.common.exceptions.IncorrectPasswordException;
import tp1.common.exceptions.InvalidUserIdException;
import tp1.common.exceptions.RequestTimeoutException;
import tp1.server.soap.SoapUtils;

public class SoapUsersClient implements UsersServerClient {
    SoapUsers server;
    public SoapUsersClient(String uri){
        server = ClientUtils.buildSoapClient(uri,
                SoapUsers.NAMESPACE, SoapUsers.NAME, SoapUsers.class);
    }
    @Override
    public synchronized User getUser(String userId, String password) throws InvalidUserIdException, IncorrectPasswordException, RequestTimeoutException {
        try {
            return ClientUtils.reTry(()->server.getUser(userId, password));
        } catch (Exception e) {
            switch (e.getMessage()){
                case SoapUtils.BAD_REQUEST -> throw new RequestTimeoutException();
                case SoapUtils.FORBIDDEN -> throw new IncorrectPasswordException();
                case SoapUtils.NOT_FOUND -> throw new InvalidUserIdException();
                default -> throw new RuntimeException(e);
            }
        }
    }

    @Override
    public synchronized boolean hasUser(String userId) throws RequestTimeoutException {
        try {
            getUser(userId, "");
        } catch (IncorrectPasswordException ignored) {
        } catch (InvalidUserIdException e) {
            return false;
        }
        return true;
    }
}
