package tp1.server.rest;

import java.util.logging.Logger;
import tp1.common.services.DirectoryService;
import tp1.common.services.UsersService;
import tp1.server.ServerUtils;
import tp1.server.rest.resources.RestUsersResource;
import tp1.tokens.TokenManager;

public class RESTUsersServer {

	private static Logger Log = Logger.getLogger(RESTUsersServer.class.getName());

    public static final int PORT = 8080;


    public static void main(String[] args) {
        ServerUtils.expect(Log, args, "secret");
        TokenManager.setSecret(args[0]);
        RestUtils.startServer(UsersService.NAME,
                RestUsersResource::new, new String[]{DirectoryService.NAME}, PORT, Log);
    }
}
