package app.service.report;

import app.model.*;
import app.model.abuseipdb.AbuseIPDBReport;
import app.model.virustotal.VirusTotalAnalyzeData;
import app.service.AbuseIPDBDService;
import app.service.VirusTotaService;
import app.util.NetAddressUtil;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Map.entry;

@Slf4j
@ApplicationScoped
public class IpSecurityReport
{
    @Inject
    AbuseIPDBDService abuseIPDBDService;

    @Inject
    VirusTotaService virusTotaService;

    public static final Map<Integer, AttackTypeTitle> TYPES = Map.ofEntries(
            entry(1, new AttackTypeTitle("DNS Compromise", "компрометации DNS")),
            entry(2, new AttackTypeTitle("DNS Poisoning", "отравлении DNS")),
            entry(3, new AttackTypeTitle("Fraud Orders", "выполнении мошеннических заказов")),
            entry(4, new AttackTypeTitle("DDoS Attack", "DDoS-атаках")),
            entry(5, new AttackTypeTitle("FTP Brute-Force", "брутфорсе FTP")),
            entry(6, new AttackTypeTitle("Ping of Death", "выполнении атаки 'пинг смерти'")),
            entry(7, new AttackTypeTitle("Phishing", "фишинге")),
            entry(8, new AttackTypeTitle("Fraud VoIP", "VoIP-мошенничестве")),
            entry(9, new AttackTypeTitle("Open Proxy", "является открытым прокси")),
            entry(10, new AttackTypeTitle("Web Spam", "веб-спаме")),
            entry(11, new AttackTypeTitle("Email Spam", "email-спаме")),
            entry(12, new AttackTypeTitle("Blog Spam", "спаме в блогах")),
            entry(13, new AttackTypeTitle("VPN IP", "является адресом VPN")),
            entry(14, new AttackTypeTitle("Port Scan", "сканировании портов")),
            entry(15, new AttackTypeTitle("Hacking", "хакинге")),
            entry(16, new AttackTypeTitle("SQL Injection", "выполнении SQL-инъекций")),
            entry(17, new AttackTypeTitle("Spoofing", "спуфинге")),
            entry(18, new AttackTypeTitle("Brute-Force", "брутфорс-атаках")),
            entry(19, new AttackTypeTitle("Bad Web Bot", "деятельности как вредоносный бот")),
            entry(20, new AttackTypeTitle("Exploited Host", "является взломанным хостом")),
            entry(21, new AttackTypeTitle("Web App Attack", "атаках на веб-приложение")),
            entry(22, new AttackTypeTitle("SSH", "злоупотреблении SSH")),
            entry(23, new AttackTypeTitle("IoT Targeted", "атаках на IoT"))
    );

    public Uni<String> attackReportForDomain(NslookupReport report) {
        if (report.getAddressIdDns().isEmpty()) {
            return Uni.createFrom().item(report.getReport());
        }

        Map.Entry<String, List<String>> entry = report.getAddressIdDns().entrySet().iterator().next();
        String domain = entry.getKey();
        List<String> ips = entry.getValue();

        return virusTotaService.getDomainAnalyzeData(domain)
                .onFailure().recoverWithItem((VirusTotalAnalyzeData) null)
                .onItem().transformToUni(domainData -> {

                    List<Uni<String>> ipTasks = ips.stream()
                            .map(this::getAttackReportByIp)
                            .toList();

                    return Uni.join().all(ipTasks).andCollectFailures()
                            .onItem().transform(ipReports -> {
                                StringBuilder sb = new StringBuilder(report.getReport());

                                String rating = (domainData != null) ? domainData.getRating() : "N/A";
                                sb.append("\nПоказатель выявления вредоносов вендорами АВПО для доменного имени %s: %s\n"
                                        .formatted(NetAddressUtil.defang(domain), rating));

                                ipReports.forEach(reportText -> {
                                    sb.append("\n").append(reportText);
                                });

                                return sb.toString();
                            });
                });
    }

    public Uni<String> getAttackReportByIp(String ip)
    {
        return Uni.combine().all().unis(
                    abuseIPDBDService.getIpCheck(ip),
                    abuseIPDBDService.getIpReports(ip),
                    virusTotaService.getIpAnalyzeData(ip)
                ).asTuple().onItem().transform(tuple -> {
                    return attackReport(new IpSecurityInfo(
                            ip,
                            tuple.getItem1(),
                            tuple.getItem2(),
                            tuple.getItem3()
                            )
                    );
                })
                .onFailure().recoverWithItem(ex -> {
                    log.error("Error getting attackReport for IP: {}", ip, ex);
                    return "Error getting attackReport for - " + ip + ": " + ex.getMessage();
                });
    }

    private String attackReport(IpSecurityInfo ipSecurityInfo)
    {
        String report;
        if (ipSecurityInfo.getVirusTotalData().getDangerRating() == 0
                && !ipSecurityInfo.getAbuseIPDBCheck().isReported())
        {
            report = "По информации из открытых источников хост %s во вредоносной активности не замечен."
                    .formatted(NetAddressUtil.defang(ipSecurityInfo.getIpAddress()));
        }

        else if (ipSecurityInfo.getVirusTotalData().getDangerRating() != 0
                && !ipSecurityInfo.getAbuseIPDBCheck().isReported())
        {
            report =  """
                По информации из открытых источников для хоста %s:
                 %s;
                 - в базах abuseipdb не числится;
                """.formatted(NetAddressUtil.defang(ipSecurityInfo.getIpAddress()),
                    antiMalwareRating(ipSecurityInfo.getVirusTotalData())
            );
        }

        else
        {
            report = fillOpenSourceReport(ipSecurityInfo);
        }

        if (ipSecurityInfo.getAbuseIPDBCheck().isTor())
        {
            report = report.concat(" - является TOR нодой;");
        }

        return report.contains("\n")
                ? report.replaceFirst(".$", ".")
                : report;
    }

    private String fillOpenSourceReport(IpSecurityInfo ipSecurityInfo) {

        return """
                По информации из открытых источников для хоста %s:
                 %s;
                 - рейтинг abuseipdb: %s%%;
                 %s;
                """.formatted(NetAddressUtil.defang(ipSecurityInfo.getIpAddress()),
                antiMalwareRating(ipSecurityInfo.getVirusTotalData()),
                ipSecurityInfo.getAbuseIPDBCheck().getScore(),
                createAttacksReport(ipSecurityInfo.getAbuseIPDBReport())
        );
    }

    private String antiMalwareRating(VirusTotalAnalyzeData virusTotalData) {
        return "- показатель выявления вредоносов вендорами АВПО для ip: %s".formatted(virusTotalData.getRating());
    }

    private String createAttacksReport(AbuseIPDBReport abuseIPDBReport) {
        if (abuseIPDBReport.data == null
                || abuseIPDBReport.data.results == null
                || abuseIPDBReport.data.results.isEmpty()) {
            return "- в сетевых атаках не замечен";
        }

        Map<Integer, Long> categoryCounts = abuseIPDBReport.data.results.stream()
                .flatMap(result -> result.categories.stream())
                .collect(Collectors.groupingBy(cat -> cat, Collectors.counting()));

        String topAttacks = categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> {
                    AttackTypeTitle title = TYPES.getOrDefault(
                            entry.getKey(), new AttackTypeTitle("Unknown", "неизвестно"));
                    return String.format("%s", title.getRu());
                })
                .collect(Collectors.joining(", "));

        return "- замечен в: " + topAttacks;
    }
}
