package tp1.kafka.operations;

import com.google.gson.*;
import tp1.kafka.RecordProcessor;

import java.util.logging.Logger;

public final class OperationUtils {
    public static final String TYPE_FIELD = "type";
    public static final String OBJ_FIELD = "object";
    private static final Gson json = new Gson();
    private OperationUtils(){}

    public static String serialize(Operation op){
        JsonObject wrapper = new JsonObject();
        wrapper.add(TYPE_FIELD, new JsonPrimitive(op.opName()));
        JsonElement object = json.toJsonTree(op);
        wrapper.add(OBJ_FIELD, object);
        return json.toJson(wrapper);
    }

    public static Operation deserialize(String op){
        JsonObject element = JsonParser.parseString(op).getAsJsonObject();
        return switch (element.get(TYPE_FIELD).getAsString()){
            case Create.NAME -> json.fromJson(element.getAsJsonObject(OBJ_FIELD), Create.class);
            case Delete.NAME -> json.fromJson(element.getAsJsonObject(OBJ_FIELD), Delete.class);
            case Edit.NAME -> json.fromJson(element.getAsJsonObject(OBJ_FIELD), Edit.class);
            case Move.NAME -> json.fromJson(element.getAsJsonObject(OBJ_FIELD), Move.class);
            case Share.NAME -> json.fromJson(element.getAsJsonObject(OBJ_FIELD), Share.class);
            case Unshare.NAME -> json.fromJson(element.getAsJsonObject(OBJ_FIELD), Unshare.class);
            default -> throw new IllegalArgumentException("No operation of that type");
        };
    }

    static String replicasToString(Iterable<String> replicas){
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        boolean first = true;
        for(String replica : replicas){
            if(first){
                first = false;
            } else{
                builder.append(",");
            }
            builder.append(replica);
        }
        builder.append("]");
        return builder.toString();
    }
}
