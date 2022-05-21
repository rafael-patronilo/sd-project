package tp1.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class Subscriber {
    private static final String REPLAY_FROM_BEGINNING = "earliest";
    private final List<String> topics;
    private final int groupId;

    private KafkaConsumer<String, String> consumer;

    public Subscriber(int groupId, String topic){
        this.topics = List.of(topic);
        this.groupId = groupId;
        initialize();
    }

    public Subscriber(int groupId, List<String> topics){
        this.topics = topics;
        this.groupId = groupId;
        initialize();
    }

    private void initialize(){
        Properties props = KafkaUtils.initializeProperties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, REPLAY_FROM_BEGINNING);
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(topics);
        consumer.poll(Duration.ofSeconds(10));
    }
}
