package tp1.serverProxies;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import tp1.serverProxies.exceptions.RequestTimeoutException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.lang.Thread.sleep;


public final class ClientUtils {
    public static final int READ_TIMEOUT = 10000;
    public static final int CONNECT_TIMEOUT = 10000;
    public static final int MAX_RETRIES = 3;
    public static final int RETRY_SLEEP = 1000;
    public static final int RETRY_ASYNC_SLEEP = 5000;

    private ClientUtils(){}
    private static Client client = null;
    public static Client getClient(){
        if(client == null){
            ClientConfig config = new ClientConfig();
            config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
            config.property( ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);
            client = ClientBuilder.newClient(config);
        }
        return client;
    }

    public static <T> T reTry(Supplier<T> func) throws RequestTimeoutException {
        for (int i = 0; i < MAX_RETRIES; i++)
            try {
                return func.get(); // Success
            } catch (ProcessingException x) {
                try {
                    sleep(RETRY_SLEEP);
                } catch (InterruptedException ignored) {}
            }
        throw new RequestTimeoutException();
    }

    public static <T> void reTryAsync(Supplier<T> func, Predicate<T> endCondition){
        new Thread(()-> {
            while (true) {
                try {
                    T result = func.get();
                    if (endCondition.test(result)) {
                        return;
                    }
                } catch (ProcessingException x) {
                    // Move to the sleep
                }

                try {
                    sleep(RETRY_ASYNC_SLEEP);
                } catch (InterruptedException ignored) {}
            }
        });
    }

    public static WebTarget buildTarget(String uri, String path){
        try{
            return getClient().target(new URI(uri)).path(path);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
