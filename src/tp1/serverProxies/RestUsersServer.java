package tp1.serverProxies;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.serverProxies.exceptions.IncorrectPasswordException;
import tp1.serverProxies.exceptions.InvalidUserIdException;

public class RestUsersServer implements UsersServerProxy{
    private WebTarget target;
    public RestUsersServer(WebTarget target){
        this.target = target;
    }

    @Override
    public User getUser(String userId, String password) throws InvalidUserIdException, IncorrectPasswordException {
        Response r = target.path( userId )
                .queryParam(RestUsers.PASSWORD, password).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        switch (r.getStatus()) {
            case 404 -> throw new InvalidUserIdException();
            case 403 -> throw new IncorrectPasswordException();
        }
        return r.readEntity(User.class);
    }

    @Override
    public boolean hasUser(String userId) {
        Response r = target.path( userId ).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        return r.getStatus() != 404;
    }
}
