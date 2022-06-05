package tp1.common;

import java.util.Objects;

public final class ServerUtils {
    private ServerUtils(){}
    private static String uri = null;

    public static void setUri(String uri){
        Objects.requireNonNull(uri);
        ServerUtils.uri = uri;
    }

    public static String getUri(){
        if(uri == null)
            throw new IllegalStateException("getUri call before setUri");
        return uri;
    }

}
