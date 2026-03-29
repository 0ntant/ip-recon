package service;


import app.model.abuseipdb.AbuseIPDBCheck;
import app.service.AbuseIPDBDService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AbuseIPDBDServiceTest {

    @Inject
    AbuseIPDBDService service;

    @Test
    public void testGetIpDataCheck() throws Exception {
        String testIp = "45.88.183.156";

        AbuseIPDBCheck info = service.getIpCheck(testIp).await().indefinitely();

        Assertions.assertNotNull(info);
        Assertions.assertEquals(testIp, info.data.ipAddress);
        Assertions.assertTrue( info.data.isPublic);
        Assertions.assertEquals(0, info.data.abuseConfidenceScore);
        Assertions.assertEquals("IN", info.data.countryCode);
        Assertions.assertEquals("Data Center/Web Hosting/Transit", info.data.usageType);
        Assertions.assertEquals("WorkTitans B.V.", info.data.isp);
        Assertions.assertEquals("the.hosting", info.data.domain);
        Assertions.assertEquals(1, info.data.hostnames.size());
        Assertions.assertEquals("vm15617036.example.com", info.data.hostnames.get(0));
        Assertions.assertFalse(info.data.isTor);
        Assertions.assertTrue(info.isReported());
    }

    @Test
    public void testGetIpDataCheckNotReported() throws Exception {
        String testIp = "45.88.183.1";

        AbuseIPDBCheck info = service.getIpCheck(testIp).await().indefinitely();

        Assertions.assertNotNull(info);
        Assertions.assertFalse(info.isReported());
    }
}
