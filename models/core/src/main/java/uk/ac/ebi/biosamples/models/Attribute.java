package uk.ac.ebi.biosamples.models;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Attribute implements Comparable<Attribute> {

	private String key;
	private String value;
	private String iri;
	private String unit;
	
	private Attribute(){
		
	}
	
    public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public String getIri() {
		return iri;
	}
	
	/**
	 * This returns a string representation of the URL to lookup the associated ontology term iri in
	 * EBI OLS. 
	 * @return
	 */
	@JsonIgnore
	public String getIriOls() {
		if (iri == null) return null;
		
		try {
			return "http://www.ebi.ac.uk/ols/terms?iri="+URLEncoder.encode(iri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			//should never get here
			throw new RuntimeException(e);
		}		
			
	}

	public String getUnit() {
		return unit;
	}

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Attribute)) {
            return false;
        }
        Attribute other = (Attribute) o;
        return Objects.equals(this.key, other.key) 
        		&& Objects.equals(this.value, other.value)
        		&& Objects.equals(this.iri, other.iri)
        		&& Objects.equals(this.unit, other.unit);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(key, value, iri, unit);
    }

	@Override
	public int compareTo(Attribute other) {
		if (other == null) {
			return 1;
		}
		
		if (!this.key.equals(other.key)) {
			return this.key.compareTo(other.key);
		}

		if (!this.value.equals(other.value)) {
			return this.value.compareTo(other.value);
		}
		
		if (this.iri == null && other.iri != null) {
			return -1;
		}
		if (this.iri != null && other.iri == null) {
			return 1;
		}
		if (this.iri != null && other.iri != null 
				&& !this.iri.equals(other.iri)) {
			return this.iri.compareTo(other.iri);
		}

		
		if (this.unit == null && other.unit != null) {
			return -1;
		}
		if (this.unit != null && other.unit == null) {
			return 1;
		}
		if (this.unit != null && other.unit != null 
				&& !this.unit.equals(other.unit)) {
			return this.unit.compareTo(other.unit);
		}
		
		return 0;
	}
	
	static public Attribute build(String key, String value, String iri, String unit) {
		Attribute attr = new Attribute();
		attr.key = key;
		attr.value = value;
		attr.iri = iri;
		attr.unit = unit;
		return attr;
	}
}