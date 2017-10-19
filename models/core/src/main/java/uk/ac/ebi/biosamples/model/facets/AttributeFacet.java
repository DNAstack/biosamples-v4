package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.core.Relation;

@Relation(collectionRelation = "facets")
public class AttributeFacet extends Facet {

    @JsonCreator
    public AttributeFacet(@JsonProperty("label") String label, @JsonProperty("count") long count, @JsonProperty("content") LabelCountListContent content) {
        super(label, count, content);
    }

    @Override
    public FacetType getType(){
        return FacetType.ATTRIBUTE;
    }


}
