package tp1.server.rest;

import tp1.common.services.DropboxFilesService;
import tp1.common.services.FilesService;
import tp1.server.rest.resources.RestFilesResource;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RESTDropboxServer {

    private static Logger Log = Logger.getLogger(RESTDropboxServer.class.getName());

    public static final int PORT = 8080;


    public static void main(String[] args) {
        Log.setLevel(Level.FINEST);
        if(args.length < 4) {
            String arg0 = args.length >= 1 ? args[0] : "";
            String arg1 = args.length >= 2 ? args[1] : "";
            String arg2 = args.length >= 3 ? args[2] : "";

            Log.severe("Expected 4 arguments: cleanState apiKey apiSecret accessToken.\n" +
                    String.format("Got %s: %s %s %s", args.length - 1, arg0, arg1, arg2));
            return;
        }

        boolean cleanState = Boolean.parseBoolean(args[0]);
        String apiKey = args[1];
        String apiSecret = args[2];
        String accessToken = args[3];

        FilesService service = new DropboxFilesService(cleanState, apiKey, apiSecret, accessToken);
        RestUtils.startServer(FilesService.NAME,
                new RestFilesResource(service), null, PORT, Log);
    }

}
