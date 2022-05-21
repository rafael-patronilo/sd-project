package tp1.server.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.client.InsecureHostnameVerifier;
import tp1.common.WebRunnable;
import tp1.common.WebSupplier;
import tp1.common.exceptions.*;
import tp1.common.services.DirectoryService;
import tp1.common.services.UsersService;
import tp1.server.MulticastServiceDiscovery;
import tp1.server.rest.resources.RestUsersResource;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Utility class for Rest servers
 */
public final class RestUtils {
    // URI format for rest servers
    private static final String SERVER_URI_FMT = "https://%s:%s/rest";
    private RestUtils() {}

    /**
     * Handles service operation exceptions and transforms into Rest responses
     * @param call the operation to handle
     * @param Log the resource's logger
     */
    public static void handleExceptions(WebRunnable call, Logger Log){
        handleExceptions(()->{
            call.invoke();
            return null;
        }, Log);
    }

    /**
     * Handles service operation exceptions and transforms into Rest responses
     * @param call the operation to handle
     * @param Log the resource's logger
     */
    public static <T> T handleExceptions(WebSupplier<T> call, Logger Log){
        try{
            return call.invoke();
        } catch (RequestTimeoutException e) {
            Log.info("throwing BAD REQUEST: timed out");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        } catch (IncorrectPasswordException e) {
            Log.info("throwing FORBIDDEN: incorrect password");
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        } catch (NoAccessException e) {
            Log.info("throwing FORBIDDEN: no access");
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        } catch (InvalidUserIdException e) {
            Log.info("throwing NOT FOUND: invalid user id");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (UnexpectedErrorException e) {
            Log.severe("throwing BAD REQUEST: unexpected error");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        } catch (InvalidFileLocationException e) {
            Log.info("throwing NOT FOUND: invalid filename");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (InvalidArgumentException e) {
            Log.info("throwing BAD REQUEST: invalid argument");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        } catch (ConflicitingUsersException e) {
            Log.info("throwing CONFLICT: conflicting users");
            throw new WebApplicationException(Response.Status.CONFLICT);
        }
    }

    /**
     * Starts a Rest server
     * @param serviceName the name of this server's service
     * @param resource the server's resource
     * @param servicesToDiscover the other services the resource requires
     * @param port the server's port
     * @param Log the server's logger
     */
    public static <T> void startServer(String serviceName, Class<T> resource, String[] servicesToDiscover, int port, Logger Log){
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());
            System.setProperty("java.net.preferIPv4Stack", "true");
            System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");

            ResourceConfig config = new ResourceConfig();
            config.register(resource);

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format(SERVER_URI_FMT, ip, port);
            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());

            Log.info(String.format("%s Server ready @ %s\n", serviceName, serverURI));
            MulticastServiceDiscovery.startDiscovery(serviceName, serverURI, servicesToDiscover);
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
