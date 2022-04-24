package tp1.client.rest;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.client.ClientUtils;
import tp1.common.clients.UsersServerClient;
import tp1.common.exceptions.IncorrectPasswordException;
import tp1.common.exceptions.InvalidUserIdException;
import tp1.common.exceptions.RequestTimeoutException;

import static tp1.client.ClientUtils.reTrySafe;

/**
 * Rest implementation for UserServerClient
 */
public class RestUsersClient implements UsersServerClient {
    private WebTarget target;
    public RestUsersClient(String uri){
        this.target = ClientUtils.buildTarget(uri, RestUsers.PATH);
    }

    @Override
    public User getUser(String userId, String password) throws InvalidUserIdException, IncorrectPasswordException, RequestTimeoutException {
        Response r = reTrySafe(()->target.path( userId )
                .queryParam(RestUsers.PASSWORD, password).request()
                .accept(MediaType.APPLICATION_JSON)
                .get());
        switch (r.getStatus()) {
            case 404 -> throw new InvalidUserIdException();
            case 403 -> throw new IncorrectPasswordException();
        }
        return r.readEntity(User.class);
    }

    @Override
    public boolean hasUser(String userId) throws RequestTimeoutException {
        Response r = reTrySafe(()->target.path( userId ).request()
                .accept(MediaType.APPLICATION_JSON)
                .get());
        return r.getStatus() != 404;
    }
}
