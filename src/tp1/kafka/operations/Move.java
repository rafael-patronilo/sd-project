package tp1.kafka.operations;

import java.util.Objects;
import java.util.Set;

public final class Move implements FileOperation {
    public static final String NAME = "Move";

    private String userId;
    private String filename;
    private String fileId;
    private String original;
    private Set<String> replicas;

    public Move(
            String userId,
            String filename,
            // The file's id
            String fileId,
            // A server that already contains the file
            String original,
            // The set of servers that should replicate this file. Must contain original
            Set<String> replicas
    ) {
        this.fileId = fileId;
        this.original = original;
        this.replicas = replicas;
    }

    @Override
    public String opName() {
        return NAME;
    }

    @Override
    public String filename() {
        return filename;
    }

    @Override
    public String userId() {
        return userId;
    }

    @Override
    public int sizeDifference() {
        return 0;
    }

    public String fileId() {
        return fileId;
    }

    public String original() {
        return original;
    }

    public Set<String> replicas() {
        return replicas;
    }
}
