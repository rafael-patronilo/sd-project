package tp1.server;

import java.util.logging.Logger;

public final class ServerUtils {
    private ServerUtils(){}
    public static void expect(Logger Log, String[] args, String... expectedParams){
        if(args.length < expectedParams.length) {
            String msg = "Expected " + expectedParams.length + " arguments: " +
                    String.join(" ", expectedParams) + "\n" +
                    String.format("Got %s: %s", args.length - 1, String.join(" ", args));
            Log.severe(msg);
            throw new RuntimeException(msg);
        }
    }
}
