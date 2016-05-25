package uk.ac.ebi.biosamples;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

@SpringBootApplication
public class Application {

	@Bean
	public Queue queue() {
		return new Queue(Messaging.queueToBeLoaded, true);
	}

	@Bean
	public MessageConverter messageConverter() {
		return new MappingJackson2MessageConverter();
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}