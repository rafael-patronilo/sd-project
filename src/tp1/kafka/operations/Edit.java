package tp1.kafka.operations;

import java.util.Objects;

public final class Edit implements FileOperation {
    public static final String NAME = "Edit";
    private int sizeDifference;

    private String filename;
    private String userId;
    private String fileId;
    private String original;

    public Edit(
            String userId,
            String filename,
            // The file's id
            String fileId,
            int sizeDifference,
            // A server already containing the updated version of the file
            String original
    ) {
        this.userId = userId;
        this.filename = filename;
        this.fileId = fileId;
        this.sizeDifference = sizeDifference;
        this.original = original;
    }

    @Override
    public String opName() {
        return NAME;
    }

    @Override
    public String filename(){
        return filename;
    }

    @Override
    public String userId(){
        return userId;
    }

    @Override
    public int sizeDifference(){
        return sizeDifference;
    }

    public String fileId() {
        return fileId;
    }

    public String original() {
        return original;
    }

}
