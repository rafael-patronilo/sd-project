package tp1.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Publisher {
    private final String topic;
    private long offset;
    private KafkaProducer<String, String> producer;

    public Publisher(String topic){
        this.topic = topic;
        this.producer = new KafkaProducer<>(KafkaUtils.initializeProperties());
    }

    public long getOffset(){
        return offset;
    }

    public long publishAndWait(String message){
        Future<RecordMetadata> promise = producer.send(new ProducerRecord<>(topic, message));
        try {
            RecordMetadata metadata = promise.get();
            this.offset = metadata.offset();
            return this.offset;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
