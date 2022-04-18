package tp1.serverProxies;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import tp1.api.service.rest.RestDirectory;
import tp1.api.service.rest.RestUsers;
import tp1.serverProxies.exceptions.IncorrectPasswordException;
import tp1.serverProxies.exceptions.InvalidUserIdException;
import tp1.serverProxies.exceptions.RequestTimeoutException;
import static tp1.serverProxies.ClientUtils.reTry;
import static tp1.serverProxies.ClientUtils.reTryAsync;

public class RestDirClient implements DirServerProxy {
    private final WebTarget target;

    public RestDirClient(String uri){
        this.target = ClientUtils.buildTarget(uri, RestDirectory.PATH);
    }

    @Override
    public void deleteDirectoryAsync(String userId, String password){
        reTryAsync(()-> target.path(userId)
                .queryParam(RestUsers.PASSWORD, password).request()
                .delete(),
                (r)-> r.getStatus() != 400
        );
    }
}
