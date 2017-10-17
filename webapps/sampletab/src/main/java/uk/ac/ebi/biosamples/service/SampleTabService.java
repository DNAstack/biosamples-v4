package uk.ac.ebi.biosamples.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.GroupNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.SampleNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.AbstractNamedAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.AbstractRelationshipAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CharacteristicAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CommentAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.DatabaseAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.SCDNodeAttribute;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class SampleTabService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final BioSamplesClient bioSamplesClient;

	public SampleTabService(BioSamplesClient bioSamplesClient) {
		this.bioSamplesClient = bioSamplesClient;
	}
	public SampleData accessionSampleTab(SampleData sampleData, String domain, String jwt, boolean setUpdateDate) {

		//release in 100 years time
		Instant release = Instant.ofEpochMilli(LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
		Instant update = Instant.ofEpochMilli(sampleData.msi.submissionUpdateDate.getTime());
		for (SampleNode sampleNode : sampleData.scd.getNodes(SampleNode.class)) {
			String accession = sampleNode.getSampleAccession();
			String name = sampleNode.getNodeName();
			
			//only build a sample if there is at least one attribute or it has no "parent" node
			//otherwise, it is just a group membership tracking dummy
			if (sampleNode.getAttributes().size() > 0 || sampleNode.getChildNodes().size() == 0) {			
				Sample sample = Sample.build(name, accession, domain, release, update, new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
				sample = bioSamplesClient.persistSampleResource(sample, setUpdateDate).getContent();
				if (accession == null) {
					sampleNode.setSampleAccession(sample.getAccession());
				}
			}
		}
		for (GroupNode groupNode : sampleData.scd.getNodes(GroupNode.class)) {
			String accession = groupNode.getGroupAccession();
			String name = groupNode.getNodeName();
							
			//this must be the last bit to build and save the object
			Sample sample = Sample.build(name, accession, domain, release, update, new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
			sample = bioSamplesClient.persistSampleResource(sample, setUpdateDate).getContent();
			if (accession == null) {
				groupNode.setGroupAccession(sample.getAccession());
			}				
		}
		return sampleData;
	}
	
	public SampleData saveSampleTab(SampleData sampleData, String domain, String jwt, boolean setUpdateDate) {
		
		Instant release = Instant.ofEpochMilli(sampleData.msi.submissionReleaseDate.getTime());
		Instant update = Instant.ofEpochMilli(sampleData.msi.submissionUpdateDate.getTime());

		Map<String, Future<Resource<Sample>>> futureMap = new TreeMap<>();
		for (SampleNode sampleNode : sampleData.scd.getNodes(SampleNode.class)) {
			String accession = sampleNode.getSampleAccession();
			String name = sampleNode.getNodeName();

			SortedSet<Attribute> attributes = new TreeSet<>();
			SortedSet<Relationship> relationships = new TreeSet<>();
			SortedSet<ExternalReference> externalReferences = new TreeSet<>();
			
			//beware, works by side-effect
			populateAttributes(accession, sampleNode.getAttributes(), attributes, relationships, externalReferences);
			
			if (sampleNode.getSampleDescription() != null && 
					sampleNode.getSampleDescription().trim().length() > 0) {
				attributes.add(Attribute.build("description", sampleNode.getSampleDescription()));
			}			
			
			
			
			//only build a sample if there is at least one attribute or it has no "parent" node
			//otherwise, it is just a group membership tracking dummy		
			if (attributes.size()+relationships.size()+externalReferences.size() > 0
					|| sampleNode.getChildNodes().size() == 0) {
				Sample sample = Sample.build(name, accession, domain, release, update, attributes, relationships, externalReferences);
				futureMap.put(name, bioSamplesClient.persistSampleResourceAsync(sample, setUpdateDate));	
			}			
		}

		//resolve futures for submitting samples
		for (String futureName : futureMap.keySet()) {
			Sample sample;
			try {
				sample = futureMap.get(futureName).get().getContent();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
			SampleNode sampleNode = sampleData.scd.getNode(futureName, SampleNode.class);
			String accession = sampleNode.getSampleAccession();
			//if it didn't have an accession before, assign one now
			if (accession == null) {
				sampleNode.setSampleAccession(sample.getAccession());
			}
		}
		
		for (GroupNode groupNode : sampleData.scd.getNodes(GroupNode.class)) {
			String accession = groupNode.getGroupAccession();
			String name = groupNode.getNodeName();

			SortedSet<Attribute> attributes = new TreeSet<>();
			SortedSet<Relationship> relationships = new TreeSet<>();
			SortedSet<ExternalReference> externalReferences = new TreeSet<>();
			
			//beware, works by side-effect
			populateAttributes(accession, groupNode.getAttributes(), attributes, relationships, externalReferences);
				
			//add relationships from the group node to any member samples
			for (Node node : groupNode.getParentNodes()) {
				//log.info("Found parent");
				if (node instanceof SampleNode) {
					SampleNode sampleNode = (SampleNode) node;
					relationships.add(Relationship.build(accession, "has member", sampleNode.getSampleAccession()));
					//log.info("Adding relationship from "+accession+" to "+sampleNode.getSampleAccession());
				}
			}		
			
			//this must be the last bit to build and save the object
			Sample sample = Sample.build(name, accession, domain, release, update, attributes, relationships, externalReferences);
			sample = bioSamplesClient.persistSampleResource(sample, setUpdateDate).getContent();
			if (accession == null) {
				groupNode.setGroupAccession(sample.getAccession());
			}				
		}
		return sampleData;
	}
	
	/**
	 * Works by side effect!
	 * 
	 * Converts the List<SCDNodeAttribute> into the passed SortedSet objects.
	 *   
	 * 
	 * @param scdNodeAttributes
	 */
	private void populateAttributes(String accession, List<SCDNodeAttribute> scdNodeAttributes, 
			SortedSet<Attribute> attributes , SortedSet<Relationship> relationships, SortedSet<ExternalReference> externalReferences) {		
		for (SCDNodeAttribute attribute : scdNodeAttributes) {
			String type = null;
			String value = null;
			String unit = null;
			
			if (attribute instanceof CommentAttribute) {
				CommentAttribute commentAttribute = (CommentAttribute) attribute;					
				type = commentAttribute.type;
				value = commentAttribute.getAttributeValue();					
				if (commentAttribute.unit != null 
						&& commentAttribute.unit.getAttributeValue() != null
						&& commentAttribute.unit.getAttributeValue().trim().length() > 0) {
					unit = commentAttribute.unit.getAttributeValue().trim();
				}					
				String termSourceId = commentAttribute.getTermSourceID();					
				attributes.add(makeAttribute(type, value, termSourceId, unit));		
				
			} else if (attribute instanceof CharacteristicAttribute) {
				CharacteristicAttribute characteristicAttribute = (CharacteristicAttribute) attribute;					
				type = characteristicAttribute.type;
				value = characteristicAttribute.getAttributeValue();					
				if (characteristicAttribute.unit != null 
						&& characteristicAttribute.unit.getAttributeValue() != null
						&& characteristicAttribute.unit.getAttributeValue().trim().length() > 0) {
					unit = characteristicAttribute.unit.getAttributeValue().trim();
				}					
				String termSourceId = characteristicAttribute.getTermSourceID();					
				attributes.add(makeAttribute(type, value, termSourceId, unit));	

			} else if (attribute instanceof AbstractNamedAttribute) {
				AbstractNamedAttribute abstractNamedAttribute = (AbstractNamedAttribute) attribute;		
				type = abstractNamedAttribute.getAttributeType();
				value = abstractNamedAttribute.getAttributeValue();	
				String termSourceId = abstractNamedAttribute.getTermSourceID();					
				attributes.add(makeAttribute(type, value, termSourceId, null));				
			} else if (attribute instanceof DatabaseAttribute) {
				DatabaseAttribute databaseAttribute = (DatabaseAttribute) attribute;
				if (databaseAttribute.databaseURI != null) {
					externalReferences.add(ExternalReference.build(databaseAttribute.databaseURI));
				}				
			} else if (attribute instanceof AbstractRelationshipAttribute) {
				//this is a relationship, store appropriately
				AbstractRelationshipAttribute abstractRelationshipAttribute = (AbstractRelationshipAttribute) attribute;
				type = abstractRelationshipAttribute.getAttributeType().toLowerCase();
				value = abstractRelationshipAttribute.getAttributeValue();
				relationships.add(Relationship.build(accession, type, value));
			}				
		}		
	}
	
	private Attribute makeAttribute(String type, String value, String termSourceId, String unit) {
		String uri = null;
		if (termSourceId != null && termSourceId.trim().length() > 0) {
			//if we're given a full uri, use it
			try {
				uri = termSourceId;
			} catch (IllegalArgumentException e) {
				//do nothing
			}
			if (uri == null) {
				//provided termSourceId wasn't a uri
				//TODO query OLS to get the URI for a short form http://www.ebi.ac.uk/ols/api/terms?id=EFO_0000001
			}
		}		
		return Attribute.build(type, value, uri, unit);
	}
}
