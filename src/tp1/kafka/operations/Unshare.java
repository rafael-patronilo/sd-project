package tp1.kafka.operations;

public record Unshare(String ownerId, String filename, String userId) implements DirOperation {
    public static final String NAME = "Unshare";

    @Override
    public String opName() {
        return NAME;
    }
}
