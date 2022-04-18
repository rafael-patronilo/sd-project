package tp1.server;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.server.resources.DirectoryResource;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

public class RESTDirServer {

    private static Logger Log = Logger.getLogger(RESTDirServer.class.getName());


    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String SERVICE = "directory";
    private static final String SERVER_URI_FMT = "http://%s:%s/rest";

    public static void main(String[] args) {
        try {

            ResourceConfig config = new ResourceConfig();
            config.register(DirectoryResource.class);

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format(SERVER_URI_FMT, ip, PORT);
            JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config);

            Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
            MulticastServiceDiscovery discovery = MulticastServiceDiscovery.getInstance();
            discovery.announcementThread(SERVICE, serverURI).start();
            discovery.addServicesToDiscover(RESTUsersServer.SERVICE, RESTFilesServer.SERVICE);
            discovery.discoveryThread().start();


        } catch( Exception e) {
            Log.severe(e.getMessage());
        }
    }
}