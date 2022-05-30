package tp1.kafka.operations;

import java.util.Set;

public record Move(
        // The file's id
        String fileId,
        // A server that already contains the file
        String original,
        // The set of servers that should replicate this file. Must contain original
        Set<String> replicas
) implements FileOperation {
    public static final String NAME = "Move";
    @Override
    public String opName() {
        return NAME;
    }
}
