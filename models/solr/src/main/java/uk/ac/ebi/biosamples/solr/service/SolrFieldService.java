package uk.ac.ebi.biosamples.solr.service;

import com.google.common.io.BaseEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.facets.FacetType;

import java.io.UnsupportedEncodingException;
import java.util.AbstractMap;
import static java.util.Map.Entry;

/**
 * SolrFieldService is the service that should be able to deal with all field matters
 * - Encode and decode of a field is the main reason behind it
 */
@Service
public class SolrFieldService {

    Logger log = LoggerFactory.getLogger(getClass());

    public String encodeFieldName(String field) {
        //solr only allows alphanumeric field types
        try {
            field = BaseEncoding.base32().encode(field.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        //although its base32 encoded, that include = which solr doesn't allow
        field = field.replaceAll("=", "_");

        return field;
    }

    public String decodeFieldName(String encodedField) {
        //although its base32 encoded, that include = which solr doesn't allow
        String decodedField = encodedField.replace("_", "=");
        try {
            decodedField = new String(BaseEncoding.base32().decode(decodedField), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return decodedField;
    }

    /**
     * Provide the encoded name of the field using a specific facet type
     * @param field the regular field name
     * @param facetType the facet type
     * @return the encoded field with specific type suffix
     */
    public String encodedField(String field, FacetType facetType) {

        // Dates fields (update and release) are not encoded at the moment
        if (facetType == FacetType.DATE) {
            return field + facetType.getSolrSuffix();
        }
        return this.encodeFieldName(field) + facetType.getSolrSuffix();
    }

    /**
     * Try to decode a field guessing the facet type
     * @param field encoded version of the field with the type suffix
     * @return the field name decoded
     */
    public Entry<FacetType, String> decodeField(String field) {
        FacetType facetType = FacetType.ofField(field);
        if (facetType == null) {
            throw new RuntimeException("Unknown type for facet field " + field);
        }

        // Remove the facet type suffix from the solr facet field
        String solrEncodedFieldName = field.replaceFirst(
                facetType.getSolrSuffix() + "$",
                "");
        // Dates fields (update and release) are not encoded at the moment
        if (facetType == FacetType.DATE) {
            return new AbstractMap.SimpleEntry<FacetType, String>(facetType, solrEncodedFieldName);
        }
        return new AbstractMap.SimpleEntry<>(facetType, this.decodeFieldName(solrEncodedFieldName));
    }

    public FieldInfo getFieldInfo(Entry<String, Long> facetField) {
        String encodedFieldName = facetField.getKey();
        Long facetFieldSampleCount = facetField.getValue();
        Entry<FacetType, String> fieldTypeAndName = decodeField(encodedFieldName);
        FacetType fieldType = fieldTypeAndName.getKey();
        String decodedFieldName = fieldTypeAndName.getValue();

        return new FieldInfo(encodedFieldName, decodedFieldName, fieldType, facetFieldSampleCount);
    }

    public static class FieldInfo {

            private final FacetType type;
            private final String fieldName;
            private final String encodedField;
            private final Long sampleCount;


            public FieldInfo(String encodedField,
                      String fieldName,
                      FacetType fieldType,
                      Long sampleCount) {
                this.type = fieldType;
                this.fieldName = fieldName;
                this.encodedField = encodedField;
                this.sampleCount = sampleCount;
            }

            public FacetType getType() {
                return type;
            }

            public String getFieldName() {
                return fieldName;
            }

            public String getEncodedField() {
                return encodedField;
            }

            public Long getSampleCount() {
                return sampleCount;
            }

    }
}
