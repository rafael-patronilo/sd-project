package tp1.client.rest;

import jakarta.ws.rs.client.WebTarget;
import tp1.api.service.rest.RestDirectory;
import tp1.api.service.rest.RestUsers;
import tp1.client.ClientUtils;
import tp1.common.clients.DirServerClient;

import static tp1.client.ClientUtils.reTryAsync;

/**
 * Rest implementation for DirServerClient
 */
public class RestDirClient implements DirServerClient {
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
