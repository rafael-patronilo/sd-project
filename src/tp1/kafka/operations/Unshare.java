package tp1.kafka.operations;

import java.util.Objects;

public final class Unshare implements Operation {
    public static final String NAME = "Unshare";
    private final String userId;
    private final String filename;
    private final String sharedWith;

    public Unshare(String userId, String filename, String sharedWith) {
        this.userId = userId;
        this.filename = filename;
        this.sharedWith = sharedWith;
    }

    @Override
    public String opName() {
        return NAME;
    }

    public String sharedWith() {
        return sharedWith;
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
