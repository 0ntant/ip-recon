package app.model.abuseipdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbuseIPDBReport {
    public Data data;

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        public List<Result> results;
        public int total;
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("reportedAt")
        public String reportedAt;

        @JsonProperty("categories")
        public List<Integer> categories;

        @JsonProperty("reporterId")
        public int reporterId;

        @JsonProperty("reporterCountryCode")
        public String reporterCountryCode;
    }

    public List<Result> getResults() {
        return (data != null) ? data.results : null;
    }

    public Integer getFirstReporterId() {
        return (data != null && data.results != null && !data.results.isEmpty())
                ? data.results.get(0).reporterId : null;
    }
}
