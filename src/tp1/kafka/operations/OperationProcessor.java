package tp1.kafka.operations;

public interface OperationProcessor {
    void onReceive(Operation op);
}
