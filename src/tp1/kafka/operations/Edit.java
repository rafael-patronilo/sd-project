package tp1.kafka.operations;

public record Edit(
        // The file's id
        String fileId,
        // A server already containing the updated version of the file
        String original
) implements FileOperation {
    public static final String NAME = "Edit";
    @Override
    public String opName() {
        return NAME;
    }
}
