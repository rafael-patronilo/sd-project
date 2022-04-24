package tp1.server.soap;

import jakarta.xml.ws.Endpoint;
import tp1.common.exceptions.*;
import tp1.server.MulticastServiceDiscovery;

import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SoapUtils {
    private static final String URI_FORMAT = "http://%s:%s/soap";
    private SoapUtils() {}

    public static final String CONFLICT = "Conflict";
    public static final String NOT_FOUND = "Not Found";
    public static final String BAD_REQUEST = "Bad Request";
    public static final String FORBIDDEN = "Forbidden";


    public static void startServer(Object resource, String serviceName, String[] servicesToDiscover, int port, Logger Log){
        try {
            /*
            System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
            System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
            System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
            System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");*/

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format(URI_FORMAT, ip, port);

            Endpoint.publish(serverURI.replace(ip, "0.0.0.0"), resource);

            MulticastServiceDiscovery.startDiscovery(serviceName, serverURI, servicesToDiscover);

            Log.info(String.format("%s Soap Server ready @ %s\n", serviceName, serverURI));
        } catch (Exception e){
            Log.severe(e.getMessage());
        }
    }


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
