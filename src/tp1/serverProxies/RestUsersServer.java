package tp1.serverProxies;

import jakarta.ws.rs.client.WebTarget;
import tp1.api.User;
import tp1.serverProxies.exceptions.IncorrectPasswordException;
import tp1.serverProxies.exceptions.InvalidUserIdException;

public class RestUsersServer implements UsersServerProxy{
    private WebTarget target;
    public RestUsersServer(WebTarget target){
        this.target = target;
    }

    @Override
    public User getUser(String userId, String password) throws InvalidUserIdException, IncorrectPasswordException {
        return null;
    }

    @Override
    public boolean hasUser(String userId) {
        return false;
    }
}
