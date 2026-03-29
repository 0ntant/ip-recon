package app.model.virustotal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class VirusTotalData {

    public Data data;

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        public String id;
        public String type;
        public Attributes attributes;
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attributes {
        @JsonProperty("as_owner")
        public String asOwner;

        @JsonProperty("last_analysis_stats")
        public LastAnalysisStats lastAnalysisStats;
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LastAnalysisStats {
        public int malicious;
        public int suspicious;
        public int undetected;
        public int harmless;
        public int timeout;
    }

    public int getDangerRating() {
        return this.getMaliciousCount() + this.getSuspiciousCount();
    }

    public String getRating() {
        return "%s/%s".formatted(
                this.getMaliciousCount() + this.getSuspiciousCount(),
                this.getMaliciousCount()
                        + this.getSuspiciousCount()
                        + this.getUndetectedCount()
                        + this.getHarmlessCount()
        );
    }

    public String getIp() {
        return data != null ? data.id : null;
    }

    public String getAsOwner() {
        return (data != null && data.attributes != null) ? data.attributes.asOwner : null;
    }

    public int getMaliciousCount() {
        return (data != null && data.attributes != null && data.attributes.lastAnalysisStats != null)
                ? data.attributes.lastAnalysisStats.malicious : 0;
    }

    public int getSuspiciousCount() {
        return (data != null && data.attributes != null && data.attributes.lastAnalysisStats != null)
                ? data.attributes.lastAnalysisStats.suspicious : 0;
    }

    public int getUndetectedCount() {
        return (data != null && data.attributes != null && data.attributes.lastAnalysisStats != null)
                ? data.attributes.lastAnalysisStats.undetected : 0;
    }

    public int getHarmlessCount() {
        return (data != null && data.attributes != null && data.attributes.lastAnalysisStats != null)
                ? data.attributes.lastAnalysisStats.harmless : 0;
    }
}
