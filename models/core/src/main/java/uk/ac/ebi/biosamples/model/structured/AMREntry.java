package uk.ac.ebi.biosamples.model.structured;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;

@JsonDeserialize(builder = AMREntry.Builder.class)
public class AMREntry implements Comparable<AMREntry>{
//    "antibiotic": "ampicillin",
//    "resistance_phenotype": "susceptible",
//    "measurement_sign": "==",
//    "measurementValue": 2,
//    "measurement_units": "mg/L",
//    "vendor": "in-house",
//    "laboratory_typing_method": "MIC",
//    "testing_standard": "CLSI"

    private final String antibiotic;
    private final String resistancePhenotype;
    private final String measurementSign;
    private final int measurementValue;
    private final String measurementUnit;
    private final String vendor;
    private final String laboratoryTypingMethod;
    private final String testingStandard;

    private AMREntry(String antibiotic, String resistancePhenotype, String measurementSign, int measurementValue, String measurementUnit, String vendor, String laboratoryTypingMethod, String testingStandard) {
        this.antibiotic = antibiotic;
        this.resistancePhenotype = resistancePhenotype;
        this.measurementSign = measurementSign;
        this.measurementValue = measurementValue;
        this.measurementUnit = measurementUnit;
        this.vendor = vendor;
        this.laboratoryTypingMethod = laboratoryTypingMethod;
        this.testingStandard = testingStandard;
    }


    public String getAntibiotic() {
        return antibiotic;
    }

    public String getResistancePhenotype() {
        return resistancePhenotype;
    }

    public String getMeasurementSign() {
        return measurementSign;
    }

    public int getMeasurementValue() {
        return measurementValue;
    }

    public String getMeasurementUnit() {
        return measurementUnit;
    }

    public String getVendor() {
        return vendor;
    }

    public String getLaboratoryTypingMethod() {
        return laboratoryTypingMethod;
    }

    public String getTestingStandard() {
        return testingStandard;
    }

        @Override
        public int compareTo(AMREntry other) {
            if (other == null) {
                return 1;
            }

            int comparison = nullSafeStringComparison(this.antibiotic, other.antibiotic);
            if (comparison != 0) {
                return comparison;
            }

            comparison = nullSafeStringComparison(this.resistancePhenotype, other.resistancePhenotype);
            if (comparison != 0) {
                return comparison;
            }

            comparison = nullSafeStringComparison(this.measurementSign, other.measurementSign);
            if (comparison != 0) {
                return comparison;
            }

            comparison = this.measurementValue - other.measurementValue;
            if (comparison != 0) {
                return comparison/Math.abs(comparison);
            }

            comparison = nullSafeStringComparison(this.measurementUnit, other.measurementUnit);
            if (comparison != 0) {
                return comparison;
            }

            comparison = nullSafeStringComparison(this.laboratoryTypingMethod, other.laboratoryTypingMethod);
            if (comparison != 0) {
                return comparison;
            }

            comparison = nullSafeStringComparison(this.vendor, other.vendor);
            if (comparison != 0) {
                return comparison;
            }

            return nullSafeStringComparison(this.testingStandard, other.testingStandard);
        }

    private int nullSafeStringComparison(String one, String two) {

        if (one == null && two != null) {
            return -1;
        }
        if (one != null && two == null) {
            return 1;
        }
        if (one != null && !one.equals(two)) {
            return one.compareTo(two);
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AMREntry)) return false;
        AMREntry amrEntry = (AMREntry) o;
        return getMeasurementValue() == amrEntry.getMeasurementValue() &&
                Objects.equals(getAntibiotic(), amrEntry.getAntibiotic()) &&
                Objects.equals(getResistancePhenotype(), amrEntry.getResistancePhenotype()) &&
                Objects.equals(getMeasurementSign(), amrEntry.getMeasurementSign()) &&
                Objects.equals(getMeasurementUnit(), amrEntry.getMeasurementUnit()) &&
                Objects.equals(getVendor(), amrEntry.getVendor()) &&
                Objects.equals(getLaboratoryTypingMethod(), amrEntry.getLaboratoryTypingMethod()) &&
                Objects.equals(getTestingStandard(), amrEntry.getTestingStandard());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getAntibiotic(), getResistancePhenotype(), getMeasurementSign(), getMeasurementValue(), getMeasurementUnit(), getVendor(), getLaboratoryTypingMethod(), getTestingStandard());
    }

    public static class Builder {
        private String antibiotic;
        private String resistancePhenotype;
        private String measurementSign;
        private Integer measurementValue;
        private String measurementUnit;
        private String vendor;
        private String laboratoryTypingMethod;
        private String testingStandard;

        @JsonCreator
        public Builder() { }

        @JsonProperty
        public Builder withAntibiotic(String antibiotic) {
            this.antibiotic = antibiotic;
            return this;
        }

        @JsonProperty
        public Builder withResistancePhenotype(String resistancePhenotype) {
            this.resistancePhenotype = resistancePhenotype;
            return this;
        }

        @JsonIgnore
        public Builder withMeasure(String sign, int value, String unit) {
            this.measurementSign = sign;
            this.measurementValue = value;
            this.measurementUnit = unit;
            return this;
        }

        @JsonProperty
        public Builder withMeasurementSign(String sign) {
            this.measurementSign = sign;
            return this;
        }

        @JsonProperty
        public Builder withMeasurementUnit(String unit) {
            this.measurementUnit = unit;
            return this;
        }

        @JsonProperty("measurement")
        public Builder withMeasurementValue(int value) {
            this.measurementValue = value;
            return this;
        }

        @JsonProperty
        public Builder withVendor(String vendor) {
            this.vendor = vendor;
            return this;
        }

        @JsonProperty
        public Builder withLaboratoryTypingMethod(String method) {
            this.laboratoryTypingMethod = method;
            return this;
        }

        @JsonProperty
        public Builder withTestingStandard(String standard) {
            this.testingStandard = standard;
            return this;
        }

        public AMREntry build() {
            if (this.antibiotic == null || this.antibiotic.isEmpty()) {
                throw AMREntryBuldingException.createForMissingField("antibiotic");
            }

            if (this.resistancePhenotype == null || this.resistancePhenotype.isEmpty()) {
                throw AMREntryBuldingException.createForMissingField("resistance phenotype");
            }

            if (this.measurementValue == null) {
                throw AMREntryBuldingException.createForMissingField("measurementValue sign");
            }

            if (this.measurementSign == null || this.measurementSign.isEmpty()) {
                throw AMREntryBuldingException.createForMissingField("measurementValue sign");
            }
            if (this.measurementUnit == null || this.measurementUnit.isEmpty()) {
                throw AMREntryBuldingException.createForMissingField("measurementValue unit");
            }

            if (this.vendor == null || this.vendor.isEmpty()) {
                throw AMREntryBuldingException.createForMissingField("vendor");
            }
            if (this.laboratoryTypingMethod == null || this.laboratoryTypingMethod.isEmpty()) {
                throw AMREntryBuldingException.createForMissingField("laboratory typing method");
            }

            if (this.testingStandard == null || this.testingStandard.isEmpty()) {
                throw AMREntryBuldingException.createForMissingField("testing standard");
            }

            return new AMREntry(this.antibiotic, this.resistancePhenotype, this.measurementSign, this.measurementValue,
                    this.measurementUnit, this.vendor, this.laboratoryTypingMethod, this.testingStandard);
        }

    }

    public static class AMREntryBuldingException extends Exception {

        public AMREntryBuldingException(String message) {
            super(message);
        }

        public static RuntimeException createForMissingField(String field) {
            return new RuntimeException("You need to provide a non-empty  " + field);
        }


    }


}
