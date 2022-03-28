package tp1.serverProxies;

import jakarta.ws.rs.client.WebTarget;
import tp1.serverProxies.exceptions.FileNotFoundException;
import tp1.serverProxies.exceptions.RequestTimeoutException;

public class RestFilesServer implements FilesServerProxy{
    private WebTarget target;
    private String uri;

    public RestFilesServer(WebTarget target, String uri){
        this.target = target;
        this.uri = uri;
    }

    @Override
    public String getUri(){
        return uri;
    }

    @Override
    public void writeFile(String fileId, byte[] data, String token) throws FileNotFoundException, RequestTimeoutException {

    }

    @Override
    public void tryDeleteFile(String fileId, String token) {

    }

    @Override
    public void redirectToGetFile(String fileId, String token) {

    }
}
