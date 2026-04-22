package app.service.report;

import app.model.*;
import app.util.NetAddressUtil;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class ReportService {
    @Inject
    WhoisReportService whoisReportService;

    @Inject
    IpSecurityReport ipSecurityReport;

    @Inject
    NslookupReportService nslookupReportService;

    public Uni<String> securityReport(String address){
        if (NetAddressUtil.isIp(address)) {
            return securityReportByIp(address);
        }
        else if (NetAddressUtil.isDomain(address))
        {
            return securityReportByDomain(address);
        }

        return Uni.createFrom().failure(
                new IllegalArgumentException(
                        "Invalid address format for security report: " + address)
        );
    }

    public Uni<String> whoIsLikeReport(String address){
        if (NetAddressUtil.isIp(address)) {
            return whoIsLikeReportByIp(address);
        }
        else if (NetAddressUtil.isDomain(address))
        {
            return whoIsLikeReportByDomain(address);
        }

        return Uni.createFrom().failure(
                new IllegalArgumentException(
                        "Invalid address format for whoIs like report: " + address)
        );
    }

    private Uni<String> securityReportByDomain(String address) {
        return nslookupReportService.nslookupReport(address)
                .onItem().transformToUni(item -> {
                    return ipSecurityReport.attackReportForDomain(item);
                });
    }

    private Uni<String> whoIsLikeReportByDomain(String address) {
        return nslookupReportService.nslookupReport(address)
                .onItem().transformToUni(item -> {
                    List<String> allIps = item.getAddressIdDns().values().stream()
                            .flatMap(List::stream)
                            .distinct()
                            .toList();

                    if (allIps.isEmpty()) {
                        return Uni.createFrom().item("");
                    }

                    List<Uni<String>> unis = allIps.stream()
                            .map(this::whoIsLikeReportByIp)
                            .collect(Collectors.toList());

                    return Uni.combine().all().unis(unis)
                            .with(results -> results.stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining("\n\n")));
                });
    }

    private Uni<String> whoIsLikeReportByIp(String address) {
        return whoisReportService.whoIsLikeReport(address);
    }

    private Uni<String> securityReportByIp(String address) {
        return ipSecurityReport.getAttackReportByIp(address);
    }
}
