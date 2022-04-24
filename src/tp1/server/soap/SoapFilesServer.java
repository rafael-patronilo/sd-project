package tp1.server.soap;

import tp1.common.services.FilesService;
import tp1.server.soap.resources.SoapFilesResource;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SoapFilesServer {
    public static final int PORT = 8080;

    private static Logger Log = Logger.getLogger(SoapFilesServer.class.getName());

    public static void main(String[] args){
        Log.setLevel(Level.INFO);
        SoapUtils.startServer(new SoapFilesResource(),
                FilesService.NAME, null, PORT, Log);

    }
}
