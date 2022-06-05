package tp1.server.soap;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jakarta.xml.ws.Endpoint;
import tp1.client.InsecureHostnameVerifier;
import tp1.common.ServerUtils;
import tp1.common.exceptions.*;
import tp1.server.MulticastServiceDiscovery;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Utility class for Soap servers
 */
public final class SoapUtils {

    // URI format for soap servers
    private static final String URI_FORMAT = "https://%s:%s/soap";
    private SoapUtils() {}

    // error messages for soap exceptions
    public static final String CONFLICT = "Conflict";
    public static final String NOT_FOUND = "Not Found";
    public static final String BAD_REQUEST = "Bad Request";
    public static final String FORBIDDEN = "Forbidden";

    /**
     * Starts a soap server
     * @param resourceSupplier a function that creates the resource for this server
     * @param serviceName the server's service name
     * @param servicesToDiscover the other services required by the resourceSupplier
     * @param port the server's port
     * @param Log the server's logger
     */
    public static void startServer(Supplier<Object> resourceSupplier, String serviceName, String[] servicesToDiscover, int port, Logger Log){
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());
            /*
            System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
            System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
            System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
            System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");*/

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format(URI_FORMAT, ip, port);
            ServerUtils.setUri(serverURI);

            HttpsServer server = HttpsServer.create(new InetSocketAddress(ip, port), 0);

            server.setExecutor(Executors.newCachedThreadPool());
            server.setHttpsConfigurator(new HttpsConfigurator(SSLContext.getDefault()));

            Endpoint endpoint = Endpoint.create(resourceSupplier.get());
            endpoint.publish(server.createContext("/soap"));

            server.start();

            MulticastServiceDiscovery.startDiscovery(serviceName, serverURI, servicesToDiscover);

            Log.info(String.format("%s Soap Server ready @ %s\n", serviceName, serverURI));
        } catch (Exception e){
            //Log.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Logs and exception and returns the respective message
     * @param e the exception to log
     * @param Log the resource's logger
     * @return the exception appropriate message
     */
    public static String logException(Exception e, Logger Log){
        if(e instanceof RequestTimeoutException){
            Log.info("throwing" + BAD_REQUEST + ": timed out");
            return BAD_REQUEST;
        } else if (e instanceof IncorrectPasswordException) {
            Log.info("throwing" + FORBIDDEN + ": incorrect password");
            return FORBIDDEN;
        } else if (e instanceof NoAccessException) {
            Log.info("throwing" + FORBIDDEN + ": no access");
            return FORBIDDEN;
        } else if (e instanceof InvalidUserIdException) {
            Log.info("throwing" + NOT_FOUND + ": invalid user id");
            return NOT_FOUND;
        } else if (e instanceof UnexpectedErrorException) {
            Log.info("throwing" + BAD_REQUEST + ": unexpected error");
            return BAD_REQUEST;
        } else if (e instanceof InvalidFileLocationException) {
            Log.info("throwing" + NOT_FOUND + ": invalid filename");
            return NOT_FOUND;
        } else if (e instanceof InvalidArgumentException) {
            Log.info("throwing" + BAD_REQUEST + ": invalid argument");
            return BAD_REQUEST;
        } else if (e instanceof ConflicitingUsersException) {
            Log.info("throwing" + CONFLICT + ": conflicting users");
            return CONFLICT;
        }
        return null;
    }


}
