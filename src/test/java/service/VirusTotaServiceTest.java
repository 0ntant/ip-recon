package service;

import app.model.virustotal.VirusTotalAnalyzeData;
import app.model.virustotal.VirusTotalAnalyzeID;
import app.model.virustotal.VirusTotalData;
import app.service.VirusTotaService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class VirusTotaServiceTest {
    @Inject
    VirusTotaService service;

    @Test
    public void testGetIpDataFormat() throws Exception {
        String testIp = "45.88.183.156";

        VirusTotalData info = service.getIpData(testIp).await().indefinitely();

        Assertions.assertNotNull(info);
        Assertions.assertEquals(1, info.getMaliciousCount());
        Assertions.assertEquals(0, info.getSuspiciousCount());
        Assertions.assertEquals(35, info.getUndetectedCount());
        Assertions.assertEquals(58, info.getHarmlessCount());
    }

    @Test
    public void testGetAnalyze() throws Exception {
        String id = "i-a6549bd8be4a93402ee15381ae3ca389f1a0bc58a404e6814382c32645131926-1774345461";

        VirusTotalAnalyzeData info = service.getAnalyzeData(id).await().indefinitely();

        Assertions.assertNotNull(info);
        Assertions.assertEquals("completed", info.getStatus());
        Assertions.assertEquals(3, info.getMaliciousCount());
        Assertions.assertEquals(0, info.getSuspiciousCount());
        Assertions.assertEquals(31, info.getUndetectedCount());
        Assertions.assertEquals(60, info.getHarmlessCount());
    }

    @Test
    public void testGetAnalyzeId() throws Exception {
        String address = "102.130.113.9";

        VirusTotalAnalyzeID info = service.getAnalyzeDomainId(address).await().indefinitely();

        Assertions.assertNotNull(info);
        Assertions.assertNotNull(info.getId());
        System.out.println(info.getId());
    }


    @Test
    public void testGetAnalyzeIpResult() throws Exception {
        String address = "102.130.113.9";

        VirusTotalAnalyzeData info = service.getIpAnalyzeData(address).await().indefinitely();

        Assertions.assertNotNull(info);
        Assertions.assertEquals("completed", info.getStatus());
        Assertions.assertTrue(info.getMaliciousCount() >= 0 );
        Assertions.assertTrue(info.getSuspiciousCount() >= 0 );
        Assertions.assertTrue(info.getUndetectedCount() >= 0);
        Assertions.assertTrue(info.getHarmlessCount() >= 0);
    }

    @Test
    public void testGetAnalyzeDomainResult() throws Exception {
        String address = "www.dvgups.ru";

        VirusTotalAnalyzeData info = service.getDomainAnalyzeData(address).await().indefinitely();

        Assertions.assertNotNull(info);
        Assertions.assertEquals("completed", info.getStatus());
        Assertions.assertTrue(info.getMaliciousCount() >= 0 );
        Assertions.assertTrue(info.getSuspiciousCount() >= 0 );
        Assertions.assertTrue(info.getUndetectedCount() >= 0);
        Assertions.assertTrue(info.getHarmlessCount() >= 0);
    }
}
