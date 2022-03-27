package tp1.server;

import java.io.IOException;
import java.net.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class MulticastServiceDiscovery {
    private static Logger Log = Logger.getLogger(MulticastServiceDiscovery.class.getName());
    private MulticastServiceDiscovery() {
    }

    private static final int DISCOVERY_PERIOD = 500;
    private static final int MAX_DATAGRAM_SIZE = 1024;
    private static final byte[] DISCOVERY_ADDRESS = new byte[]{(byte) 226, (byte) 226, (byte) 226, (byte) 226};
    private static final int DISCOVERY_PORT = 2266;
    private static final String DELIMITER = "\t";

    public static Thread announcementThread(String service, String uri) {
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

    public static Thread discoveryThread(Consumer<String[]> callback) {
        DatagramPacket packet = new DatagramPacket(new byte[MAX_DATAGRAM_SIZE], MAX_DATAGRAM_SIZE);
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
                                Log.info(String.format("FROM %s (%s) : %s\n", packet.getAddress().getCanonicalHostName(),
                                        packet.getAddress().getHostAddress(), msg));
                                if(tokens.length == 2){
                                    Log.info(String.format("Discovered %s at %s", tokens[0], tokens[1]));
                                    callback.accept(tokens);
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
}
