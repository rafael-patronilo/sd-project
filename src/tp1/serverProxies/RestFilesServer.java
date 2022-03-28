package tp1.serverProxies;

import jakarta.ws.rs.client.WebTarget;

public class RestFilesServer implements FilesServerProxy{
    private WebTarget target;

    public RestFilesServer(WebTarget target){
        this.target = target;
    }

}
