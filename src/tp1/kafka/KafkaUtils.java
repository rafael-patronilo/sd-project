package tp1.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

final class KafkaUtils {
    static final String KAFKA_BROKERS = "localhost:9092, kafka:9092";

    private KafkaUtils(){}

    /**
     * Initializes the properties common to publisher and subscriber
     * @return
     */
    public static Properties initializeProperties(){
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return props;
    }
}
