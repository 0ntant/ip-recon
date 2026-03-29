package app.model;

import app.model.abuseipdb.AbuseIPDBCheck;
import app.model.abuseipdb.AbuseIPDBReport;
import app.model.virustotal.VirusTotalAnalyzeData;
import app.model.virustotal.VirusTotalData;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;

@RegisterForReflection
@AllArgsConstructor
@Getter
public class IpSecurityInfo {
    String ipAddress;
    AbuseIPDBCheck abuseIPDBCheck;
    AbuseIPDBReport abuseIPDBReport;
    VirusTotalAnalyzeData virusTotalData;
}
