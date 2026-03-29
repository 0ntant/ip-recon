package app.service;

import app.model.abuseipdb.AbuseIPDBCheck;
import app.model.abuseipdb.AbuseIPDBReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@ApplicationScoped
public class AbuseIPDBDService {
    private static final String BASE_URL = "https://api.abuseipdb.com/api/v2";

    @ConfigProperty(name = "abuseipdb.api-key")
    String API_KEY;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @CacheResult(cacheName = "abuseipdb-check-cache")
    public Uni<AbuseIPDBCheck> getIpCheck(String ip) {
        String url = BASE_URL + "/check?maxAgeInDays=90&ipAddress=" + ip;
        return sendRequestAsync(url, AbuseIPDBCheck.class);
    }

    @CacheResult(cacheName = "abuseipdb-reports-cache")
    public Uni<AbuseIPDBReport> getIpReports(String ip) {
        String url = BASE_URL + "/reports?page=1&perPage=25&maxAgeInDays=365&ipAddress=" + ip;
        return sendRequestAsync(url, AbuseIPDBReport.class);
    }

    private <T> Uni<T> sendRequestAsync(String url, Class<T> clazz) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Key", API_KEY)
                .GET()
                .build();

        return Uni.createFrom().completionStage(() -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .ifNoItem().after(Duration.ofSeconds(50)).fail()
                .onItem().transform(response -> {
                    logResponse(request, response);
                    if (response.statusCode() == 429) {
                        throw new RuntimeException("AbuseIPDB limit exceeded (429)");
                    }
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("AbuseIPDB error: " + response.statusCode());
                    }
                    return response;
                })
                .onFailure().retry()
                .withBackOff(Duration.ofSeconds(20), Duration.ofSeconds(50))
                .atMost(5)
                .onItem().transform(response -> {
                    try {
                        return objectMapper.readValue(response.body(), clazz);
                    } catch (Exception e) {
                        throw new RuntimeException("Mapping error for url: " + url, e);
                    }
                })
                .onFailure().transform(failure ->
                        new RuntimeException("AbuseIPDBService failed after retries for url: " + url, failure)
                );
    }

    private void logResponse(HttpRequest request, HttpResponse<String> response) {
        log.info("[{}] {} | ResponseCode: {}", request.method(), request.uri(), response.statusCode());
    }
}
