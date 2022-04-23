package tp1.server.rest;

import tp1.common.services.DirectoryService;
import tp1.common.services.FilesService;
import tp1.common.services.UsersService;
import tp1.server.rest.resources.RestDirResource;
import java.util.logging.Logger;

public class RESTDirServer {

    private static Logger Log = Logger.getLogger(RESTDirServer.class.getName());

    public static final int PORT = 8080;

    public static void main(String[] args) {
        RestUtils.startServer(DirectoryService.NAME,
                RestDirResource.class, new String[]{UsersService.NAME, FilesService.NAME}, PORT, Log);
    }
}
