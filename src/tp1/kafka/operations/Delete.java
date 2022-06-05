package tp1.kafka.operations;

import java.util.Objects;

public final class Delete implements FileOperation, Operation {
    public static final String NAME = "Delete";
    private String userId;
    private String filename;
    private String fileId;

    public Delete(
            // The user who owns this file
            String userId,
            // The file's name
            String filename,
            // The file's id
            String fileId
    ) {
        this.userId = userId;
        this.filename = filename;
        this.fileId = fileId;
    }

    @Override
    public String opName() {
        return NAME;
    }

    public String userId() {
        return userId;
    }

    @Override
    public int sizeDifference() {
        return 0;
    }

    public String filename() {
        return filename;
    }

    public String fileId() {
        return fileId;
    }

}
