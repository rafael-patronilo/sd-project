package tp1.kafka.operations;

public record Share(String ownerId, String filename, String userId) implements DirOperation{
    public static final String NAME = "Share";
    @Override
    public String opName() {
        return NAME;
    }
}
