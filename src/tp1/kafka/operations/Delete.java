package tp1.kafka.operations;

public record Delete(
        // The user who owns this file
        String userId,
        // The file's name
        String filename,
        // The file's id
        String fileId
) implements DirOperation, FileOperation {
    public static final String NAME = "Delete";
    @Override
    public String opName() {
        return NAME;
    }
}
