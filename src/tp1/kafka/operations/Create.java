package tp1.kafka.operations;

import java.util.Set;


public record Create(
        // The user who owns this file
        String userId,
        // The file's name
        String filename,
        // The file's id
        String fileId,
        // A server that already contains the file
        String original,
        // The set of servers that should replicate this file. Must contain original
        Set<String> replicas
) implements FileOperation, DirOperation {
    public static final String NAME = "Create";
    @Override
    public String opName() {
        return NAME;
    }
}
