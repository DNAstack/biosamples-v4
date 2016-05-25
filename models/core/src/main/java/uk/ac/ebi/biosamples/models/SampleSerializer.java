package uk.ac.ebi.biosamples.models;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class SampleSerializer extends JsonSerializer<Sample> {

	@Override
	public void serialize(Sample sample, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		gen.writeStartObject();
		gen.writeStringField("accession", sample.getAccession());
		gen.writeStringField("name", sample.getName());
		
		gen.writeStringField("update", sample.getUpdateDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
		gen.writeStringField("release", sample.getReleaseDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
		
		gen.writeArrayFieldStart("attributes");
		for (String key : sample.getAttributeTypes()) {
			for (String value : sample.getAttributeValues(key)) {
				gen.writeStartObject();
				gen.writeStringField("key", key);
				gen.writeStringField("value", value);
				if (sample.getAttributeUnit(key, value) != null) {
					gen.writeStringField("unit", sample.getAttributeUnit(key, value));
				}
				if (sample.getAttributeOntologyTerm(key, value) != null) {
					gen.writeStringField("ontologyTerm", sample.getAttributeOntologyTerm(key, value).toString());
				}
				gen.writeEndObject();
			}
		}
		gen.writeEndArray();
		gen.writeEndObject();

	}

}