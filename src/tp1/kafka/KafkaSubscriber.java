package tp1.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import tp1.kafka.operations.OperationProcessor;
import tp1.kafka.operations.OperationUtils;

public class KafkaSubscriber {
	private Logger Log = Logger.getLogger(KafkaSubscriber.class.getName());
	static public KafkaSubscriber createSubscriber(String brokers, List<String> topics, String mode) {

		Properties props = new Properties();

		// Localização dos servidores kafka (lista de máquinas + porto)
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);

		// Configura o modo de subscrição (ver documentação em kafka.apache.org)
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, mode);

		// Configura o grupo de consumidores (ver documentação em kafka.apache.org)
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "grp" + System.nanoTime());

		// Classe para serializar as chaves dos eventos (string)
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

		// Classe para serializar os valores dos eventos (string)
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

		// Cria um consumidor (assinante/subscriber)
		return new KafkaSubscriber(new KafkaConsumer<String, String>(props), topics);
	}

	private static final long POLL_TIMEOUT = 1L;

	final KafkaConsumer<String, String> consumer;

	public KafkaSubscriber(KafkaConsumer<String, String> consumer, List<String> topics) {
		this.consumer = consumer;
		this.consumer.subscribe(topics);
	}

	public void startWithOp(boolean block, OperationProcessor processor){
		start(block, opToRecordProcessor(processor));
	}

	public void start(boolean block, RecordProcessor processor) {
		if( block )
			consume( processor );
		else
			new Thread( () -> consume( processor)).start();
	}

	/**
	 * Polls the records once for POLL_TIMEOUT seconds
	 * @param processor the operation processor to use
	 */
	public void consumeOnceOperation(OperationProcessor processor) {
		consumeOnce(opToRecordProcessor(processor));
	}

	/**
	 * Wraps a record processor around an operation processor
	 * @param processor the operation processor
	 * @return the record processor
	 */
	private RecordProcessor opToRecordProcessor(OperationProcessor processor){
		return (record) -> processor.onReceive(OperationUtils.deserialize(record.value()), record.offset());
	}

	/**
	 * Polls the records once for POLL_TIMEOUT seconds
	 * @param processor the record processor to use
	 */
	public void consumeOnce(RecordProcessor processor) {
		consumer.poll(Duration.ofSeconds(POLL_TIMEOUT)).forEach(r -> {
			Log.info("Received: version = "  + r.offset() + ", value = " + r.value());
			processor.onReceive(r);
		});
	}

	private void consume(RecordProcessor processor) {
		for (;;)
			consumeOnce(processor);
	}
}
