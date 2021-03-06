package uk.ac.ebi.biosamples.ncbi;

import org.dom4j.Element;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ncbi.service.NcbiSampleConversionService;

@Service
public class NcbiElementCallableFactory {

	private final BioSamplesClient bioSamplesClient;

	private final String domain;

	private final NcbiSampleConversionService conversionService;

	public NcbiElementCallableFactory(NcbiSampleConversionService conversionService, BioSamplesClient bioSamplesClient, PipelinesProperties pipelinesProperties) {
		this.conversionService = conversionService;
		this.bioSamplesClient = bioSamplesClient;
		this.domain = pipelinesProperties.getNcbiDomain();
	}
	
	
	public NcbiElementCallable build(Element element) {
		return new NcbiElementCallable(conversionService, bioSamplesClient, element, domain);
	}
}
