package tp1.server.rest;

import tp1.common.services.FilesService;
import tp1.server.rest.resources.RestFilesResource;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RESTFilesServer {
    private static Logger Log = Logger.getLogger(RESTFilesServer.class.getName());

    public static final int PORT = 8080;


    public static void main(String[] args) {
        Log.setLevel(Level.FINEST);
        RestUtils.startServer(FilesService.NAME,
                RestFilesResource::new, null, PORT, Log);
    }
}
