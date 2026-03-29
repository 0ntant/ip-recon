package app.model.abuseipdb;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbuseIPDBCheck {

    public Data data;

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        public String ipAddress;
        public boolean isPublic;
        public int ipVersion;
        public boolean isWhitelisted;
        public int abuseConfidenceScore;
        public String countryCode;
        public String usageType;
        public String isp;
        public String domain;
        public List<String> hostnames;
        public boolean isTor;
        public String countryName;
        public int totalReports;
        public int numDistinctUsers;
        public String lastReportedAt;
    }

    public boolean isReported() {
        return data != null && data.lastReportedAt != null;
    }

    public String getIp() {
        return data != null ? data.ipAddress : null;
    }

    public int getScore() {
        return data != null ? data.abuseConfidenceScore : 0;
    }

    public String getIsp() {
        return data != null ? data.isp : null;
    }

    public boolean isTor() {
        return data != null && data.isTor;
    }
}
