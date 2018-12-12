package uk.ac.ebi.biosamples.solr.model.field;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.data.solr.core.query.Criteria;

import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.filter.DateRangeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;

@Component
public class SolrSampleDateField extends SolrSampleField{


    public SolrSampleDateField() {
        super();
    }

    public SolrSampleDateField(String readableLabel) {
        super(readableLabel);
    }

    /**
     * All subclasses should implement this constructor
     *
     * @param readableLabel
     * @param solrDocumentLabel
     */
    public SolrSampleDateField(String readableLabel, String solrDocumentLabel) {
        super(readableLabel, solrDocumentLabel);
    }

    @Override
    public Pattern getSolrFieldPattern() {
        return Pattern.compile("^(?<fieldname>release|update)(?<fieldsuffix>"+getSolrFieldSuffix()+")$");
    }

    @Override
    public String getSolrFieldSuffix() {
        return "_dt";
    }

    @Override
    public boolean isEncodedField() {
        return false;
    }

    @Override
    public boolean isCompatibleWith(Filter filter) {
        return filter instanceof DateRangeFilter;
    }

    @Override
    public FacetFetchStrategy getFacetCollectionStrategy() {
        throw new RuntimeException("Method not yet implemented");
    }

    @Override
    public Criteria getFilterCriteria(Filter filter) {
        Criteria filterCriteria = null;

        if (filter instanceof DateRangeFilter) {

            DateRangeFilter dateRangeFilter = (DateRangeFilter) filter;
            filterCriteria = new Criteria(this.getSolrLabel());

            if (dateRangeFilter.getContent().isPresent()) {
                DateRangeFilter.DateRange dateRange = dateRangeFilter.getContent().get();
                if (dateRange.isFromMinDate() && dateRange.isUntilMaxDate()) {
                    filterCriteria = filterCriteria.isNotNull();
                } else if (dateRange.isFromMinDate()) {
                    filterCriteria = filterCriteria.lessThanEqual(toSolrDateString(dateRange.getUntil()));
                } else if (dateRange.isUntilMaxDate()){
                    filterCriteria = filterCriteria.greaterThanEqual(toSolrDateString(dateRange.getFrom()));
                } else {
                    filterCriteria = filterCriteria.between(
                    		toSolrDateString(dateRange.getFrom()),
                    		toSolrDateString(dateRange.getUntil()),
                    		true, false);
                }

            } else {
                filterCriteria = filterCriteria.isNotNull();
            }
        }

        return filterCriteria;
    }


    private String toSolrDateString(TemporalAccessor temporal) {
        return DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(temporal);
    }
}
