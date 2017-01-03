package uk.ac.ebi.biosamples.ncbi;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.apache.http.client.utils.URIBuilder;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.UriTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.models.Attribute;
import uk.ac.ebi.biosamples.models.Relationship;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.utils.HateoasUtils;
import uk.ac.ebi.biosamples.utils.XMLUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NCBIElementCallable implements Callable<Void> {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	private final Element sampleElem;

	@Autowired
	private XMLUtils xmlUtils;
	
	@Autowired
	private HateoasUtils hateoasUtils;
	
	@Autowired
	private PipelinesProperties pipelinesProperties;
	
	//use RestOperations as the interface implemented by RestTemplate
	//easier to mock for testing
	@Autowired
	private RestOperations restTemplate;

	public NCBIElementCallable(Element sampleElem) {
		this.sampleElem = sampleElem;
	}

	private void submit(Sample sample) {
		//all NCBI samples have an existing accession
		//so its always a PUT to that accession
		
		URI putUri = UriComponentsBuilder.fromUri(pipelinesProperties.getBiosampleSubmissionURI()).path("samples").build().toUri();
		
		log.info("PUTing "+sample.getAccession());
		//was there, so we need to PUT an update
		
		HttpEntity<Sample> requestEntity = new HttpEntity<>(sample);
		ResponseEntity<Resource<Sample>> putResponse = restTemplate.exchange(putUri,
				HttpMethod.PUT,
				requestEntity,
				new ParameterizedTypeReference<Resource<Sample>>(){});
		
		if (!putResponse.getStatusCode().is2xxSuccessful()) {
			log.error("Unable to PUT "+sample.getAccession()+" : "+putResponse.toString());
			throw new RuntimeException("Problem PUTing "+sample.getAccession());
		}
	}

	@Override
	public Void call() throws Exception {

		String accession = sampleElem.attributeValue("accession");

		log.trace("Element callable starting for "+accession);
		
		// TODO compare to last version of XML?

		// convert it to our model

		Element description = xmlUtils.getChildByName(sampleElem, "Description");


		String name = xmlUtils.getChildByName(description, "Title").getTextTrim();
		// if the name is double quotes, strip them
		if (name.startsWith("\"")) {
			name = name.substring(1, name.length()).trim();
		}
		if (name.endsWith("\"")) {
			name = name.substring(0, name.length()-1).trim();
		}
		// if the name is blank, force it
		if (name.trim().length() == 0) {
			name = accession;
		}
		
		SortedSet<Attribute> attrs = new TreeSet<>();
		SortedSet<Relationship> rels = new TreeSet<>();

		for (Element idElem : xmlUtils.getChildrenByName(xmlUtils.getChildByName(sampleElem, "Ids"), "Id")) {
			String id = idElem.getTextTrim();
			if (!accession.equals(id) && !name.equals(id)) {
				attrs.add(Attribute.build("synonym",  id,  null,  null));
			}
		}

		Element descriptionCommment = xmlUtils.getChildByName(description, "Comment");
		if (descriptionCommment != null) {
			Element descriptionParagraph = xmlUtils.getChildByName(descriptionCommment, "Paragraph");
			if (descriptionParagraph != null) {
				String secondaryDescription = descriptionParagraph.getTextTrim();
				if (!name.equals(secondaryDescription)) {
					attrs.add(Attribute.build("description", secondaryDescription,  null,  null));
				}
			}
		}

		// handle the organism
		Element organismElement = xmlUtils.getChildByName(description, "Organism");
		if (organismElement.attributeValue("taxonomy_id") == null) {
			attrs.add(Attribute.build("organism", organismElement.attributeValue("taxonomy_name").trim(),  null,  null));
		} else {
			// TODO taxonomy reference
			attrs.add(Attribute.build("organism", organismElement.attributeValue("taxonomy_name").trim(),  null,  null));
		}

		// handle attributes
		for (Element attrElem : xmlUtils.getChildrenByName(xmlUtils.getChildByName(sampleElem, "Attributes"),
				"Attribute")) {
			String key = attrElem.attributeValue("display_name");
			if (key == null || key.length() == 0) {
				key = attrElem.attributeValue("attribute_name");
			}
			String value = attrElem.getTextTrim();
			//value is a sample accession, assume its a relationship
			if (value.matches("SAM[END]A?[0-9]+")) {
				//if its a self-relationship, then dont add it
				if (!value.equals(accession)) {
					rels.add(Relationship.build(key, value, accession));
				}
			} else {
				//its an attribute
				attrs.add(Attribute.build(key, value, null, null));
			}
		}

		// handle model and packages
		for (Element modelElem : xmlUtils.getChildrenByName(xmlUtils.getChildByName(sampleElem, "Models"), "Model")) {
			attrs.add(Attribute.build("model", modelElem.getTextTrim(), null, null));
		}
		attrs.add(Attribute.build("package", xmlUtils.getChildByName(sampleElem, "Package").getTextTrim(), null, null));

		//handle dates
		LocalDateTime updateDate = null;
		updateDate = LocalDateTime.parse(sampleElem.attributeValue("last_update"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		LocalDateTime releaseDate = null;
		releaseDate = LocalDateTime.parse(sampleElem.attributeValue("publication_date"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		
		LocalDateTime latestDate = updateDate;
		if (releaseDate.isAfter(latestDate)) {
			latestDate = releaseDate;
		}
		
		//Sample sample = Sample.createFrom(name, accession, updateDate, releaseDate, keyValues, new HashMap<>(), new HashMap<>(),relationships);
		Sample sample = Sample.build(name, accession, releaseDate, updateDate, attrs, rels);
		
		//now pass it along to the actual submission process
		submit(sample);

		log.trace("Element callable finished");
		
		return null;
	}

}
