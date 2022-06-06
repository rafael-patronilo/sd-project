package tp1.server.rest;

import tp1.common.services.DropboxFilesService;
import tp1.common.services.FilesService;
import tp1.server.ServerUtils;
import tp1.server.rest.resources.RestFilesResource;
import tp1.tokens.TokenManager;

import java.util.logging.Level;
import java.util.logging.Logger;


public class RESTDropboxServer {

    private static Logger Log = Logger.getLogger(RESTDropboxServer.class.getName());

    public static final int PORT = 8080;


    public static void main(String[] args) {
        Log.setLevel(Level.FINEST);
        ServerUtils.expect(Log, args, "cleanState", "secret", "apiKey", "apiSecret", "accessToken");
        boolean cleanState = Boolean.parseBoolean(args[0]);
        TokenManager.setSecret(args[1]);
        String apiKey = args[2];
        String apiSecret = args[3];
        String accessToken = args[4];

        FilesService service = new DropboxFilesService(cleanState, apiKey, apiSecret, accessToken);
        RestUtils.startServer(FilesService.NAME,
                () -> new RestFilesResource(service), null, PORT, Log);
    }

}
