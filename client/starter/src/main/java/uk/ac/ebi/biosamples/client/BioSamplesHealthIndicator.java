package uk.ac.ebi.biosamples.client;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.BioSamplesProperties;

public class BioSamplesHealthIndicator implements HealthIndicator {

	private final RestTemplate restTemplate;
	private final URI uri;

	private Logger log = LoggerFactory.getLogger(getClass());
	
	
	public BioSamplesHealthIndicator(RestTemplateBuilder restTemplateBuilder, BioSamplesProperties bioSamplesProperties) {
		this.restTemplate = restTemplateBuilder.build();
		
		//TODO use HAL for this
		this.uri = UriComponentsBuilder
			.fromUri(bioSamplesProperties.getBiosamplesClientUri())
			.pathSegment("health")
			.build().toUri();
	}
	
	public Health health() {
		log.trace("Checking health...");
		//.accept(MediaType.parseMediaType("application/vnd.spring-boot.actuator.v1+json"))
		RequestEntity<Void> request = RequestEntity.get(uri).build();
		ResponseEntity<?> response = null;
		try {
			//by default, Health doesn't deseralize appropriately due to lack of builder
			//however, as its information is reflected in the http status code, we can use that instead
			//therefore we use void here to ignore the body of the response
			response = restTemplate.exchange(request, Void.class);
		} catch (RestClientResponseException e) {
			log.trace("health down ", e);
			return Health.down().withDetail("connection", ""+e.getRawStatusCode()).build();
		} catch (RestClientException e) {
			log.trace("health down unable to connect", e);
			return Health.down().withDetail("connection", "unable to connect").build();
		}
		
		if (response == null) {
			log.trace("health down response null");
			return Health.down().withDetail("connection", "unable to connect").build();
		} else {	
			log.trace("health up response up");	
			return Health.up().withDetail("connection", ""+response.getStatusCodeValue()).build();
		}
	}

}
