package app.service.report;

import app.model.NslookupReport;
import app.service.NslookupService;
import app.util.NetAddressUtil;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class NslookupReportService {
    @Inject
    NslookupService nslookupService;

    public Uni<NslookupReport> nslookupReport(String dnsAddress) {
        return nslookupService.getRecordsAsync(dnsAddress).onItem()
                .transform(tupe -> fillReport(dnsAddress, tupe));
    }

    private NslookupReport fillReport(String dnsAddress, List<String> ips) {
        StringBuilder reportBuilder = new StringBuilder("Домен %s разрешается в:\n"
                .formatted(NetAddressUtil.defang(dnsAddress)));

        Map<String, List<String>> addressIpDns = new HashMap<>();
        addressIpDns.put(dnsAddress, ips);

        for (String ip : ips) {
            reportBuilder.append(" - %s\n".formatted(NetAddressUtil.defang(ip)));
        }

        return new NslookupReport(addressIpDns, reportBuilder.toString());
    }
}
