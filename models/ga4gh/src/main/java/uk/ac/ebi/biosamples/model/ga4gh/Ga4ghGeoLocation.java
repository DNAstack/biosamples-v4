package uk.ac.ebi.biosamples.model.ga4gh;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class Ga4ghGeoLocation {
    private String label;
    private String precision;
    private double latitude;
    private double longtitude;
    private double altitude;

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @JsonProperty("precision")
    public String getPrecision() {
        return precision;
    }

    public void setPrecision(String precision) {
        this.precision = precision;
    }

    @JsonProperty("latitude")
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @JsonProperty("longtitude")
    public double getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(double longtitude) {
        this.longtitude = longtitude;
    }

    @JsonProperty("altitude")
    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ga4ghGeoLocation that = (Ga4ghGeoLocation) o;
        return Double.compare(that.latitude, latitude) == 0 &&
                Double.compare(that.longtitude, longtitude) == 0 &&
                Double.compare(that.altitude, altitude) == 0 &&
                Objects.equals(label, that.label) &&
                Objects.equals(precision, that.precision);
    }

    @Override
    public int hashCode() {

        return Objects.hash(label, precision, latitude, longtitude, altitude);
    }

    @Override
    protected Ga4ghGeoLocation clone() {
        try {
            return (Ga4ghGeoLocation) super.clone();
        }catch (CloneNotSupportedException e){
            return new Ga4ghGeoLocation();
        }
    }
}
