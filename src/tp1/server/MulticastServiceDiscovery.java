package tp1.server;

import tp1.common.services.DirectoryService;
import tp1.common.services.UsersService;

import javax.security.auth.callback.Callback;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class MulticastServiceDiscovery {
    private static Logger Log = Logger.getLogger(MulticastServiceDiscovery.class.getName());
    private static MulticastServiceDiscovery instance;
    public static MulticastServiceDiscovery getInstance(){
        if(instance == null){
            instance = new MulticastServiceDiscovery();
        }
        return instance;
    }

    private Map<String, Set<String>> discovered;
    private Map<String, Consumer<String>> listeners;

    private MulticastServiceDiscovery() {
    }

    private static final int DISCOVERY_PERIOD = 500;
    private static final int MAX_DATAGRAM_SIZE = 1024;
    private static final byte[] DISCOVERY_ADDRESS = new byte[]{(byte) 226, (byte) 226, (byte) 226, (byte) 226};
    private static final int DISCOVERY_PORT = 2266;
    private static final String DELIMITER = "\t";

    public Thread announcementThread(String service, String uri) {
        byte[] content = String.format("%s%s%s", service, DELIMITER, uri).getBytes();
        try {
            InetAddress address = InetAddress.getByAddress(DISCOVERY_ADDRESS);
            DatagramPacket packet = new DatagramPacket(content, content.length, address, DISCOVERY_PORT);
            return new Thread(
                    () -> {
                        try (var ds = new DatagramSocket()) {
                            while (true) {
                                try {
                                    Thread.sleep(DISCOVERY_PERIOD);
                                    ds.send(packet);
                                } catch (InterruptedException | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (SocketException e) {
                            e.printStackTrace();
                        }
                    }
            );
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initializeDiscovery(){
        if(discovered == null)
            discovered = new HashMap<>();
        if(listeners == null)
            listeners = new HashMap<>();
    }

    public void addServicesToDiscover(String[] servicesToDiscover){
        initializeDiscovery();
        for (String serviceType: servicesToDiscover) {
            discovered.putIfAbsent(serviceType, new HashSet<>());
        }
    }

    public Set<String> discoveredServices(String service){
        initializeDiscovery();
        return discovered.get(service);
    }

    public void listenForServices(String serviceType, Consumer<String> listener){
        initializeDiscovery();
        discovered.putIfAbsent(serviceType, new HashSet<>());
        listeners.put(serviceType, listener);

    }

    public Thread discoveryThread() {
        DatagramPacket packet = new DatagramPacket(new byte[MAX_DATAGRAM_SIZE], MAX_DATAGRAM_SIZE);
        initializeDiscovery();
        return new Thread(
                () -> {
                    try (var ms = new MulticastSocket(DISCOVERY_PORT)) {
                        InetAddress address = InetAddress.getByAddress(DISCOVERY_ADDRESS);
                        ms.joinGroup(address);
                        while (true) {
                            try {
                                packet.setLength(MAX_DATAGRAM_SIZE);
                                ms.receive(packet);

                                var msg = new String(packet.getData(), 0, packet.getLength());
                                var tokens = msg.split(DELIMITER);
                                Log.finest(String.format("FROM %s (%s) : %s\n",
                                        packet.getAddress().getCanonicalHostName(),
                                        packet.getAddress().getHostAddress(), msg));
                                if(tokens.length == 2){
                                    Set<String> services = discovered.get(tokens[0]);
                                    if(services != null && services.add(tokens[1])) {
                                        Consumer<String> listener = listeners.get(tokens[0]);
                                        if(listener != null)
                                            listener.accept(tokens[1]);
                                        Log.info(String.format("Discovered %s at %s", tokens[0], tokens[1]));
                                    }
                                }

                            }catch (IOException e){
                                try{
                                    Thread.sleep(DISCOVERY_PERIOD);
                                }catch(InterruptedException e1){
                                    e1.printStackTrace();
                                }
                                Log.finest("Still listening...");

                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    public static void startDiscovery(String serviceName, String uri, String[] servicesToDiscover){
        MulticastServiceDiscovery discovery = getInstance();
        discovery.announcementThread(serviceName, uri).start();
        if(servicesToDiscover != null && servicesToDiscover.length > 0) {
            discovery.addServicesToDiscover(servicesToDiscover);
            discovery.discoveryThread().start();
        }
    }
}
