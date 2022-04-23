package tp1.server.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.common.WebRunnable;
import tp1.common.WebSupplier;
import tp1.common.exceptions.*;
import tp1.common.services.DirectoryService;
import tp1.common.services.UsersService;
import tp1.server.MulticastServiceDiscovery;
import tp1.server.rest.resources.RestUsersResource;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

public final class RestUtils {
    private static final String SERVER_URI_FMT = "http://%s:%s/rest";
    private RestUtils() {}

    public static void handleExceptions(WebRunnable call, Logger Log){
        handleExceptions(()->{
            call.invoke();
            return null;
        }, Log);
    }

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

    public static <T> void startServer(String serviceName, Class<T> resource, String[] servicesToDiscover, int port, Logger Log){
        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");

            ResourceConfig config = new ResourceConfig();
            config.register(resource);

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format(SERVER_URI_FMT, ip, port);
            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

            Log.info(String.format("%s Server ready @ %s\n", serviceName, serverURI));
            MulticastServiceDiscovery.startDiscovery(serviceName, serverURI, servicesToDiscover);
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
