package tp1.tokens;

import java.util.Arrays;
import java.util.Base64;
import com.google.gson.*;
import tp1.kafka.operations.*;

/**
 * Holds the secret and handles the creation and validation of tokens
 */
public final class TokenManager {

    public static final String SEPARATOR = "_";
    private static final String TYPE_FIELD = "type";
    private static final String OBJ_FIELD = "token";

    private TokenManager(){}

    private static final Gson json = new Gson();

    private static final Base64.Encoder b64enc = Base64.getUrlEncoder();
    private static final Base64.Decoder b64dec = Base64.getUrlDecoder();
    private static String secret;
    public static void setSecret(String secret){
        TokenManager.secret = secret;
    }

    public static PermanentToken createPermanentToken(){
        return new PermanentToken(secret);
    }

    public static TemporaryToken createTemporaryToken(String fileId){
        return new TemporaryToken(secret, fileId);
    }

    public static TemporaryToken createTemporaryToken(String fileId, long timestamp){
        return new TemporaryToken(secret, fileId, timestamp);
    }

    public static boolean checkToken(Token token){
        if(token == null)
            return false;
        return token.checkToken(secret);
    }

    public static Token checkToken(String serialized){
        Token token = deserializeToken(serialized);
        if(!checkToken(token))
            return null;
        else return token;
    }

    private static String encode64(String value){
        return b64enc.encodeToString(value.getBytes());
    }

    private static String decode64(String encoded){
        return new String(b64dec.decode(encoded));
    }

    public static String serializeToken(Token token){
        /*JsonObject wrapper = new JsonObject();
        wrapper.add(TYPE_FIELD, new JsonPrimitive(token.tokenType()));
        JsonElement object = json.toJsonTree(token);
        wrapper.add(OBJ_FIELD, object);
        return encode64(json.toJson(wrapper));*/
        return token.tokenType() + SEPARATOR + String.join(SEPARATOR, token.toStrings());
    }

    public static Token deserializeToken(String serialized){
        String[] parts = serialized.split(SEPARATOR);
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        return switch (parts[0]) {
            case PermanentToken.NAME -> PermanentToken.fromStrings(args);
            case TemporaryToken.NAME -> TemporaryToken.fromStrings(args);
            default -> null;
        };
    }
}
