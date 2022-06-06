package tp1.server.soap;

import tp1.common.services.FilesService;
import tp1.server.ServerUtils;
import tp1.server.soap.resources.SoapFilesResource;
import tp1.tokens.TokenManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SoapFilesServer {
    public static final int PORT = 8080;

    private static Logger Log = Logger.getLogger(SoapFilesServer.class.getName());

    public static void main(String[] args){
        Log.setLevel(Level.INFO);
        ServerUtils.expect(Log, args, "secret");
        TokenManager.setSecret(args[0]);
        SoapUtils.startServer(SoapFilesResource::new,
                FilesService.NAME, null, PORT, Log);

    }
}
