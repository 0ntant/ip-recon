package app.model.virustotal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class VirusTotalAnalyzeData {

    @JsonProperty("data")
    private Data data;
    @JsonProperty("meta")
    private Meta meta;

    @RegisterForReflection
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String id;
        private String type;
        private Attributes attributes;
    }

    @RegisterForReflection
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attributes {
        private String status; //queued completed
        private long date;
        private Map<String, EngineResult> results;
        private Stats stats;
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class EngineResult {
        private String method;
        @JsonProperty("engine_name")
        private String engineName;
        private String category;
        private String result;

    }

    @RegisterForReflection
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Stats {
        private int malicious;
        private int suspicious;
        private int undetected;
        private int harmless;
        private int timeout;
    }

    @RegisterForReflection
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("netloc_info")
        private NetlocInfo netlocInfo;
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NetlocInfo {
        private String id;
    }

    public int getDangerRating() {
        return this.getMaliciousCount();
    }

    public String getRating() {
        return "%s/%s".formatted(
                this.getMaliciousCount(),
                this.getMaliciousCount()
                        + this.getSuspiciousCount()
                        + this.getUndetectedCount()
                        + this.getHarmlessCount()
        );
    }

    public String getStatus() {
        return data.attributes.status;
    }

    public int getMaliciousCount() {
        return (data != null && data.attributes != null && data.attributes.stats != null)
                ? data.attributes.stats.malicious : 0;
    }

    public int getSuspiciousCount() {
        return (data != null && data.attributes != null && data.attributes.stats != null)
                ? data.attributes.stats.suspicious : 0;
    }

    public int getUndetectedCount() {
        return (data != null && data.attributes != null && data.attributes.stats != null)
                ? data.attributes.stats.undetected : 0;
    }

    public int getHarmlessCount() {
        return (data != null && data.attributes != null && data.attributes.stats != null)
                ? data.attributes.stats.harmless : 0;
    }
}
