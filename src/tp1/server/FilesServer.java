package tp1.server;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.server.resources.FilesResource;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

public class FilesServer {
    public final static String SERVICE = "FilesService";
    private static Logger Log = Logger.getLogger(FilesServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;

    private static final String SERVER_URI_FMT = "http://%s:%s/rest";

    public static void main(String[] args) {
        try {

            ResourceConfig config = new ResourceConfig();
            config.register(FilesResource.class);

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format(SERVER_URI_FMT, ip, PORT);
            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
            MulticastServiceDiscovery.announcementThread(SERVICE, serverURI).start();
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
