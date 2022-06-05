package tp1.server.soap;

import tp1.common.services.DirectoryService;
import tp1.common.services.UsersService;
import tp1.server.soap.resources.SoapUserResource;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SoapUsersServer {
    public static final int PORT = 8080;

    private static Logger Log = Logger.getLogger(SoapUsersServer.class.getName());

    public static void main(String[] args){
        Log.setLevel(Level.INFO);
        SoapUtils.startServer(SoapUserResource::new,
                UsersService.NAME, new String[]{DirectoryService.NAME}, PORT, Log);
    }
}
