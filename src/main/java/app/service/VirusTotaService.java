package app.service;

import app.model.virustotal.VirusTotalAnalyzeData;
import app.model.virustotal.VirusTotalAnalyzeID;
import app.model.virustotal.VirusTotalData;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@ApplicationScoped
public class VirusTotaService {
    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "virustotal.api-key")
    String API_KEY;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @CacheResult(cacheName = "virustotal-ip-cache")
    public Uni<VirusTotalData> getIpData(String address) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.virustotal.com/api/v3/ip_addresses/" + address))
                .header("Accept", "application/json")
                .header("x-apikey", API_KEY)
                .GET()
                .build();
        return request(request, VirusTotalData.class);
    }

    @CacheResult(cacheName = "virustotal-domain-cache")
    public Uni<VirusTotalData> getDomainData(String address) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.virustotal.com/api/v3/domains/" + address))
                .header("Accept", "application/json")
                .header("x-apikey", API_KEY)
                .GET()
                .build();
        return request(request, VirusTotalData.class);
    }

    @CacheResult(cacheName = "virustotal-analyze-ip-cache")
    public Uni<VirusTotalAnalyzeData> getIpAnalyzeData(String address) {
        return getAnalyzeIpId(address)
                .chain(analyzeId -> {
                    String id = analyzeId.getId();
                    return getAnalyzeData(id)
                            .repeat()
                            .withDelay(Duration.ofSeconds(30))
                            .whilst(data -> "queued".equals(data.getStatus()))
                            .select().first(45)
                            .collect().last()
                            .onItem().transformToUni(finalData -> {
                                String status = finalData.getStatus();

                                if ("queued".equals(status)) {
                                    return Uni.createFrom().failure(
                                            new RuntimeException("Virustotal analyze for ip=" + address + " is still queued after max retries")
                                    );
                                }

                                if (!"completed".equals(status)) {
                                    return Uni.createFrom().failure(
                                            new RuntimeException("Virustotal analyze for ip=" + address + " status: " + status)
                                    );
                                }

                                log.info("Analyze complete successfully for ip={}", address);
                                return Uni.createFrom().item(finalData);
                            });
                });
    }

    @CacheResult(cacheName = "virustotal-analyze-domain-cache")
    public Uni<VirusTotalAnalyzeData> getDomainAnalyzeData(String address) {
        return getAnalyzeDomainId(address)
                .chain(analyzeId -> {
                    String id = analyzeId.getId();
                    return getAnalyzeData(id)
                            .repeat()
                            .withDelay(Duration.ofSeconds(20))
                            .whilst(data -> "queued".equals(data.getStatus()))
                            .select().first(20)
                            .collect().last()
                            .onItem().transformToUni(finalData -> {
                                String status = finalData.getStatus();

                                if ("queued".equals(status)) {
                                    return Uni.createFrom().failure(
                                            new RuntimeException("Virustotal analyze for domain=" + address + " is still queued after max retries")
                                    );
                                }

                                if (!"completed".equals(status)) {
                                    return Uni.createFrom().failure(
                                            new RuntimeException("Virustotal analyze for domain=" + address + " status: " + status)
                                    );
                                }

                                log.info("Analyze complete successfully for domain={}", address);
                                return Uni.createFrom().item(finalData);
                            });
                });
    }

    @CacheResult(cacheName = "virustotal-analyze-domain-id-cache")
    public Uni<VirusTotalAnalyzeID> getAnalyzeDomainId(String address) {
        String formData = "url=" + URLEncoder.encode(address, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.virustotal.com/api/v3/urls"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("x-apikey", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();
        return request(request, VirusTotalAnalyzeID.class);
    }

    @CacheResult(cacheName = "virustotal-analyze-ip-id-cache")
    public Uni<VirusTotalAnalyzeID> getAnalyzeIpId(String address){
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.virustotal.com/api/v3/ip_addresses/"+address+"/analyse"))
                .header("Accept", "application/json")
                .header("x-apikey", API_KEY)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return request(request,VirusTotalAnalyzeID.class);
    }

    public Uni<VirusTotalAnalyzeData> getAnalyzeData(String id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.virustotal.com/api/v3/analyses/" + id))
                .header("Accept", "application/json")
                .header("x-apikey", API_KEY)
                .GET()
                .build();
        return request(request, VirusTotalAnalyzeData.class);
    }

    private <T> Uni<T> request(HttpRequest request, Class<T> responseClass) {
        return Uni.createFrom().completionStage(() -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .ifNoItem().after(Duration.ofSeconds(12)).fail()
                .onItem().transform(response -> {
                    logResponse(request, response);
                    if (response.statusCode() == 429) {
                        throw new RuntimeException("VirusTotal API limit exceeded (429).");
                    }
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("VirusTotal API error: " + response.statusCode());
                    }
                    return response;
                })
                .onFailure().retry()
                .withBackOff(Duration.ofSeconds(20), Duration.ofSeconds(50))
                .atMost(5)
                .onItem().transform(response -> {
                    try {
                        return objectMapper.readValue(response.body(), responseClass);
                    } catch (Exception e) {
                        log.error("Error parsing VirusTotal response: {}", request.uri().toString(), e);
                        throw new RuntimeException("VirusTotal Mapping error", e);
                    }
                })
                .onFailure().transform(failure ->
                        new RuntimeException("VirusTotalService error [" + request.method() + "]: " + request.uri().toString(), failure)
                );
    }

    private void logResponse(HttpRequest request, HttpResponse<String> response) {
        log.info("[{}] {} | ResponseCode: {}",
                request.method(),
                request.uri(),
                response.statusCode());
    }
}
