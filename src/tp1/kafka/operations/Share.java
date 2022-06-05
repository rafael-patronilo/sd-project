package tp1.kafka.operations;

import java.util.Objects;

public final class Share implements Operation {
    public static final String NAME = "Share";
    private final String userId;
    private final String filename;
    private final String sharingWith;

    public Share(String userId, String filename, String sharingWith) {
        this.userId = userId;
        this.filename = filename;
        this.sharingWith = sharingWith;
    }

    @Override
    public String opName() {
        return NAME;
    }

    public String sharingWith() {
        return sharingWith;
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

}
