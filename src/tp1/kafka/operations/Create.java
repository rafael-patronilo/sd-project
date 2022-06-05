package tp1.kafka.operations;

import java.util.Objects;
import java.util.Set;


public final class Create implements FileOperation, Operation {
    public static final String NAME = "Create";
    private String userId;
    private String filename;
    private String fileId;
    private String original;
    private Set<String> replicas;

    private int size;
    public Create(
            // The user who owns this file
            String userId,
            // The file's name
            String filename,
            // The file's id
            String fileId,
            int size,
            // URI of a server that already contains the file
            String original,
            // The set of servers that should replicate this file. Must contain original
            Set<String> replicas
    ) {
        this.userId = userId;
        this.filename = filename;
        this.fileId = fileId;
        this.size = size;
        this.original = original;
        this.replicas = replicas;
    }

    @Override
    public String opName() {
        return NAME;
    }

    @Override
    public String userId() {
        return userId;
    }

    @Override
    public String filename() {
        return filename;
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

    @Override
    public int sizeDifference(){
        return size;
    }

}
