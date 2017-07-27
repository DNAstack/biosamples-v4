package uk.ac.ebi.biosamples.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.util.MultiValueMap;
import uk.ac.ebi.biosamples.model.JsonLDSample;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.JsonLDSample;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.BioSamplesAapService.SampleNotAccessibleException;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.service.JsonLDService;
import uk.ac.ebi.biosamples.service.SamplePageService;
import uk.ac.ebi.biosamples.service.SampleReadService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;

import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

/**
 * Primary controller for REST operations both in JSON and XML and both read and
 * write.
 * 
 * See {@link SampleHtmlController} for the HTML equivalent controller.
 * 
 * @author faulcon
 *
 */
@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples")
public class SampleRestController {

	private final SampleService sampleService;
	private final SamplePageService samplePageService;
	private final FilterService filterService;
	private final BioSamplesAapService bioSamplesAapService;

	private final SampleResourceAssembler sampleResourceAssembler;

	private final EntityLinks entityLinks;

    private final JsonLDService jsonLDService;

    private Logger log = LoggerFactory.getLogger(getClass());

	public SampleRestController(SampleService sampleService, 
			SamplePageService samplePageService,FilterService filterService, 
			BioSamplesAapService bioSamplesAapService,
			SampleResourceAssembler sampleResourceAssembler, 
			EntityLinks entityLinks,
            JsonLDService jsonLDService) {
		this.sampleService = sampleService;
		this.samplePageService = samplePageService;
		this.bioSamplesAapService = bioSamplesAapService;
		this.filterService = filterService;
		this.sampleResourceAssembler = sampleResourceAssembler;
		this.jsonLDService = jsonLDService;
		this.entityLinks = entityLinks;
	}

	@GetMapping(value = "/{accession}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public Resource<Sample> getSampleHal(@PathVariable String accession) {
		log.info("starting call");
		// convert it into the format to return
		Sample sample = null;
		sample = sampleService.fetch(accession);

		if (sample.getName() == null) {
			// if it has no name, then its just created by accessioning or
			// reference
			// can't read it, but could put to it
			// TODO use METHOD_NOT_ALLOWED
			// TODO make sure "options" is correct for this
			throw new SampleReadService.SampleNotFoundException(accession);
		}
		
		bioSamplesAapService.checkAccessible(sample);

		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);
		
		// create the response object with the appropriate status
		return sampleResource;
	}

    @CrossOrigin(methods = RequestMethod.GET)
    @GetMapping(value = "/{accession}", produces = "application/ld+json")
    public JsonLDSample getJsonLDSample(@PathVariable String accession) {
        Sample sample = sampleService.fetch(accession);
        if (sample.getName() == null) {
            // if it has no name, then its just created by accessioning or
            // reference
            // can't read it, but could put to it
            // TODO make sure "options" is correct for this
			throw new SampleReadService.SampleNotFoundException(accession);
        }
        
		bioSamplesAapService.checkAccessible(sample);
		
        return jsonLDService.sampleToJsonLD(sample);
    }

	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample accession must match URL accession") // 400
	public static class SampleAccessionMismatchException extends RuntimeException {
	}

    @PreAuthorize("isAuthenticated()")
	@PutMapping(value = "/{accession}", consumes = { MediaType.APPLICATION_JSON_VALUE })
	public Resource<Sample> put(@PathVariable String accession, 
			@RequestBody Sample sample) {
		
		if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
			// if the accession in the body is different to the accession in the
			// url, throw an error
			// TODO create proper exception with right http error code
			throw new SampleAccessionMismatchException();
		}		
		
		log.info("Recieved PUT for " + accession);
		sample = bioSamplesAapService.handleSampleDomain(sample);		
		sample = sampleService.store(sample);

		// assemble a resource to return
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

		// create the response object with the appropriate status
		return sampleResource;
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource<Sample>> post(@RequestBody Sample sample) {
		
		log.info("Recieved POST");
		sample = bioSamplesAapService.handleSampleDomain(sample);
		sample = sampleService.store(sample);
		
		// assemble a resource to return
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

		// create the response object with the appropriate status
		//TODO work out how to avoid using ResponseEntity but also set location header
		return ResponseEntity.created(URI.create(sampleResource.getLink("self").getHref())).body(sampleResource);
	}

}
