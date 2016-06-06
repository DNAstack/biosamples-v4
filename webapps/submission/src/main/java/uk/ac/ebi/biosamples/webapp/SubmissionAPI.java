package uk.ac.ebi.biosamples.webapp;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.models.MongoSample;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.models.SimpleSample;
import uk.ac.ebi.biosamples.repos.MongoSampleRepository;

@RestController
public class SubmissionAPI {

	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private MongoSampleRepository mongoSampleRepo;

	// @Autowired
	private RabbitMessagingTemplate rabbitTemplate;

	@Autowired
	public SubmissionAPI(RabbitMessagingTemplate rabbitTemplate, MessageConverter messageConverter) {
		this.rabbitTemplate = rabbitTemplate;
		this.rabbitTemplate.setMessageConverter(messageConverter);
		//this.rabbitTemplate.setDefaultDestination(Messaging.exchangeForLoading);
	}

	@RequestMapping(value = "/samples", method = RequestMethod.POST)
	public Sample newSample(@RequestBody Sample sample, HttpServletResponse response) throws AlreadySubmittedException {

		log.info("Recieved POST for "+sample.getAccession());
		
		// get anything that already exists from the repository
		MongoSample oldSample = mongoSampleRepo.findByAccession(sample.getAccession());

		// if something does exist, throw an exception because this is POST not
		// PUT
		if (oldSample != null) {
			throw new AlreadySubmittedException();
		}

		
		//TODO set the update date to todays date?
		
		// create the new record
		MongoSample newSample = MongoSample.createFrom(sample);

		// persist the new sample
		newSample = mongoSampleRepo.save(newSample);

		// flag response that we created a sample
		response.setStatus(HttpServletResponse.SC_CREATED);

		// put in loading message queue
		rabbitTemplate.convertAndSend(Messaging.exchangeForLoading, "", SimpleSample.createFrom(newSample));

		return newSample;
	}

	@RequestMapping(value = "/samples", method = RequestMethod.PUT)
	public Sample updateSample(@RequestBody Sample sample, HttpServletResponse response) throws NotSubmittedException {

		log.info("Recieved PUT for "+sample.getAccession());
		
		
		// get previous version from repository
		MongoSample oldSample = mongoSampleRepo.findByAccession(sample.getAccession());
		// if something does not exist, throw an exception because this is PUT
		// not POST
		if (oldSample == null) {
			throw new NotSubmittedException();
		}

		//TODO compare the sample to the old version, only accept a real change
		
		//TODO set the update date to todays date?

		// flag the old record as archived
		oldSample.doArchive();

		// create the new record
		MongoSample newSample = MongoSample.createFrom(sample);

		// persist the new and old sample
		oldSample = mongoSampleRepo.save(oldSample);
		newSample = mongoSampleRepo.save(newSample);

		// put in loading message queue
		rabbitTemplate.convertAndSend(Messaging.exchangeForLoading, "", SimpleSample.createFrom(newSample));

		return newSample;
	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Updates must have an accession")
	public class NotSubmittedException extends Exception {
		private static final long serialVersionUID = 868777054850746361L;

	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "New samples cannot have an accession")
	public class AlreadySubmittedException extends Exception {
		private static final long serialVersionUID = 502181687021078545L;

	}
}
