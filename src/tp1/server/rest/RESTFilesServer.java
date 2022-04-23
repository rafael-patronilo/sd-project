package tp1.server.rest;

import tp1.common.services.FilesService;
import tp1.server.rest.resources.RestFilesResource;
import java.util.logging.Logger;

public class RESTFilesServer {
    private static Logger Log = Logger.getLogger(RESTFilesServer.class.getName());

    public static final int PORT = 8080;


    public static void main(String[] args) {
        RestUtils.startServer(FilesService.NAME,
                RestFilesResource.class, null, PORT, Log);
    }
}
