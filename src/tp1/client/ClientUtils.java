package tp1.client;

import com.sun.xml.ws.client.BindingProviderProperties;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import tp1.common.exceptions.RequestTimeoutException;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
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
    private static Client restClient = null;

    public interface SoapSupplier<T>{
        T invoke() throws Exception;
    }

    public static void setSoapClientTimeouts(BindingProvider port ) {
        port.getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);
        port.getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, READ_TIMEOUT);
    }

    public static <T> T buildSoapClient(String uri, String namespace, String name, Class<T> cls) {
        QName qname = new QName(namespace, name);
        Service service = null;
        try {
            service = Service.create( URI.create(uri + "?wsdl").toURL(), qname);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        T server = service.getPort(cls);
        ClientUtils.setSoapClientTimeouts((BindingProvider) server);
        return server;
    }

    public static Client getRestClient(){
        if(restClient == null){
            ClientConfig config = new ClientConfig();
            config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
            config.property( ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);
            restClient = ClientBuilder.newClient(config);
        }
        return restClient;
    }

    public static <T> T reTrySafe(Supplier<T> func) throws RequestTimeoutException {
        return reTrySafe(func, MAX_RETRIES);
    }

    public static <T> T reTry(SoapSupplier<T> func) throws RequestTimeoutException, Exception {
        return reTry(func, MAX_RETRIES);
    }

    public static <T> T reTry(SoapSupplier<T> func, int maxRetries) throws RequestTimeoutException, Exception{
        for (int i = 0; i < maxRetries; i++)
            try {
                return func.invoke(); // Success
            } catch (ProcessingException | WebServiceException x) {
                try {
                    sleep(RETRY_SLEEP);
                } catch (InterruptedException ignored) {}
            }
        throw new RequestTimeoutException();
    }

    /**
     * Tries a function multiple times until it succeeds.
     * @param func a function to call with no exceptions to catch
     * @param maxRetries max number of retries
     * @return the returned value
     * @param <T> the return type
     * @throws RequestTimeoutException on timeout
     */
    public static <T> T reTrySafe(Supplier<T> func, int maxRetries) throws RequestTimeoutException{
        try {
            return reTry(func::get, maxRetries);
        } catch (RequestTimeoutException e){
            throw e;
        } catch (Exception e){
            return null; //func can't throw checked exceptions
        }
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
        }).start();
    }

    public static WebTarget buildTarget(String uri, String path){
        try{
            return getRestClient().target(new URI(uri)).path(path);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
