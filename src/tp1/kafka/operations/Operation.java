package tp1.kafka.operations;

/**
 * An operation to spread over Kafka
 */
public interface Operation {
    String opName();

    String filename();
    String userId();

    /**
     * Returns the modification to the file's size
     * @return the value that should be summed to the file's size to update it.
     */
    int sizeDifference();
}