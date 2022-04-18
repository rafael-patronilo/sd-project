package tp1.serverProxies;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tp1.api.service.rest.RestDirectory;
import tp1.api.service.rest.RestUsers;
import tp1.serverProxies.exceptions.IncorrectPasswordException;
import tp1.serverProxies.exceptions.InvalidUserIdException;
import tp1.serverProxies.exceptions.RequestTimeoutException;

public class RestDirServer implements DirServerProxy {
    private final WebTarget target;

    public RestDirServer(WebTarget target){
        this.target = target.path(RestDirectory.PATH);
    }

    @Override
    public void deleteDirectory(String userId, String password) throws InvalidUserIdException, IncorrectPasswordException, RequestTimeoutException {
        Response r = target.path(userId)
                .queryParam(RestUsers.PASSWORD, password).request()
                .delete();
    }
}
