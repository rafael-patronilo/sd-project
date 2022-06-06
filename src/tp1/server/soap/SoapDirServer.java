package tp1.server.soap;

import tp1.common.services.DirectoryService;
import tp1.common.services.FilesService;
import tp1.common.services.UsersService;
import tp1.server.ServerUtils;
import tp1.server.soap.resources.SoapDirResource;
import tp1.tokens.TokenManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SoapDirServer {
    public static final int PORT = 8080;

    private static Logger Log = Logger.getLogger(SoapDirServer.class.getName());

    public static void main(String[] args){
        Log.setLevel(Level.INFO);
        ServerUtils.expect(Log, args, "secret");
        TokenManager.setSecret(args[0]);
        SoapUtils.startServer(SoapDirResource::new,
                DirectoryService.NAME, new String[]{UsersService.NAME, FilesService.NAME}, PORT, Log);
    }
}
