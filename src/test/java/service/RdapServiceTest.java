package service;

import app.model.RdapData;
import app.service.RdapService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.format.DateTimeFormatter;

@QuarkusTest
public class RdapServiceTest {
    @Inject
    RdapService ipService;

    @Test
    public void testGetIpDataFormat() throws Exception {
        String testIp = "45.88.183.156";

        RdapData info = ipService.getIpData(testIp).await().indefinitely();

        Assertions.assertNotNull(info);
        Assertions.assertEquals("45.88.183.0 - 45.88.183.255", info.handle);
        Assertions.assertEquals("THE-HOSTING", info.name);
        Assertions.assertEquals("IN", info.country);
        Assertions.assertEquals("2025.05.28", info.getLastChangedDate());
    }


    @Test
    public void testGetIpDataFormatForTorNode() throws Exception
    {
        String testIp = "102.130.113.9";

        RdapData info = ipService.getIpData(testIp).await().indefinitely();

        Assertions.assertNotNull(info);
        Assertions.assertEquals("2018.11.22", info.getRegistrationDate());
    }
}
