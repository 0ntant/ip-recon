package app.service.report;

import app.model.abuseipdb.AbuseIPDBCheck;
import app.model.RdapData;
import app.service.AbuseIPDBDService;
import app.service.RdapService;
import app.util.NetAddressUtil;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@ApplicationScoped
public class WhoisReportService {

    @Inject
    RdapService rdapService;
    @Inject
    AbuseIPDBDService abuseIPDBDService;

    public Uni<String> whoIsLikeReport(String ip) {
        return Uni.combine().all().unis(
                rdapService.getIpData(ip),
                abuseIPDBDService.getIpCheck(ip)
        ).asTuple().onItem().transform(tuple -> {
            return fillRdapData(ip ,tuple.getItem1(), tuple.getItem2());
        })
        .onFailure().recoverWithItem(ex -> {
            log.error("Error getting whoIsReport for IP: {}", ip, ex);
            return "Error getting whoIsRePort for - " + ip + ": " + ex.getMessage();
        });
    }

    private String fillRdapData(String ip, RdapData rdapData, AbuseIPDBCheck abuseCheck) {
        List<String> lines = new ArrayList<>();
        lines.add("===== Информация о системе =====");

        String hostInfo = NetAddressUtil.defang(ip);

        if (abuseCheck != null && abuseCheck.data != null) {
            if (abuseCheck.data.hostnames != null && !abuseCheck.data.hostnames.isEmpty()) {
                hostInfo += " - " + NetAddressUtil.defang(abuseCheck.data.hostnames.get(0));
            }
        }
        lines.add("Хост-инициатор: " + hostInfo);

        if (abuseCheck != null && abuseCheck.data != null) {
            if (abuseCheck.getIsp() != null) lines.add("ISP: " + abuseCheck.getIsp());
            if (abuseCheck.data.countryCode != null) lines.add("Country: " + abuseCheck.data.countryCode);
        }

        if (rdapData != null) {
            if (rdapData.handle != null) lines.add("NetRange: " + rdapData.handle);

            String regDate = rdapData.getRegistrationDate();
            if (regDate != null) lines.add("Registration: " + regDate);

            String lastChanged = rdapData.getLastChangedDate();
            if (lastChanged != null) lines.add("Last changed: " + lastChanged);
        }

        return String.join("\n", lines);
    }
}
