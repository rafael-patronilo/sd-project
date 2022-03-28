package tp1.server;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.server.resources.DirectoryResource;
import tp1.serverProxies.FilesServerProxy;
import tp1.server.resources.UsersResource;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DirectoryServer {

    private static Logger Log = Logger.getLogger(DirectoryServer.class.getName());


    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String SERVICE = "DirectoryService";
    private static final String SERVER_URI_FMT = "http://%s:%s/rest";

    public static void main(String[] args) {
        try {

            ResourceConfig config = new ResourceConfig();
            List<FilesServerProxy> fileServers = new ArrayList<>();
            config.property(DirectoryResource.FILE_SERVERS_PROPERTY, fileServers);
            config.register(DirectoryResource.class);

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format(SERVER_URI_FMT, ip, PORT);
            JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config);

            Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
            MulticastServiceDiscovery.announcementThread(SERVICE, serverURI).start();
            MulticastServiceDiscovery.discoveryThread(
                    (tokens)-> {
                        switch (tokens[0]){
                            case UsersServer.SERVICE:
                                //TODO save users service
                                break;
                            case FilesServer.SERVICE:
                                //TODO save files service
                                break;
                            default:
                                break;
                        }
                    }).start();

        } catch( Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
