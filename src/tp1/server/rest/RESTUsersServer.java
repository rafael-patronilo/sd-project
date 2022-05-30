package tp1.server.rest;

import java.util.logging.Logger;
import tp1.common.services.DirectoryService;
import tp1.common.services.UsersService;
import tp1.server.rest.resources.RestUsersResource;

public class RESTUsersServer {

	private static Logger Log = Logger.getLogger(RESTUsersServer.class.getName());

    public static final int PORT = 8080;


    public static void main(String[] args) {
        RestUtils.startServer(UsersService.NAME,
                new RestUsersResource(), new String[]{DirectoryService.NAME}, PORT, Log);
    }
}
