package uk.ac.ebi.biosamples.mongo.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.CustomLocalDateTimeDeserializer;
import uk.ac.ebi.biosamples.service.CustomLocalDateTimeSerializer;

public class MongoSubmission {

	@Id
	@JsonIgnore
	private String id;

	@JsonSerialize(using = CustomLocalDateTimeSerializer.class)
	@JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
	@LastModifiedDate
	private LocalDateTime datetime;
	
	private String user;

	private Sample sample;
	//TODO also store submission of curation and external reference links
	
	private MongoSubmission(){}
	
	public MongoSubmission(Sample sample, LocalDateTime datetime){
		this.sample = sample;
		this.datetime = datetime;
	}

	public LocalDateTime getDatetime() {
		return datetime;
	}

	public String getUser() {
		return user;
	}

	public Sample getSample() {
		return sample;
	}
	
}
