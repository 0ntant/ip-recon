package app.service;

import app.model.RdapData;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@ApplicationScoped
public class RdapService {
    @Inject
    ObjectMapper objectMapper;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @CacheResult(cacheName = "rdap-cache")
    public Uni<RdapData> getIpData(String ip) {
        String url = "https://rdap.db.ripe.net/ip/" + ip;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Accept", "application/json").GET().build();

        return Uni.createFrom().completionStage(() -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .ifNoItem().after(Duration.ofSeconds(20)).fail()
                .onItem().transform(response -> {
                    logResponse(request, response);
                    if (response.statusCode() == 429) {
                        throw new RuntimeException("RdapService API limit exceeded (429) for IP: " + ip);
                    }
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("RdapService error: " + response.statusCode());
                    }
                    return response;
                })
                .onFailure().retry()
                .withBackOff(Duration.ofSeconds(20), Duration.ofSeconds(50))
                .atMost(5)
                .onItem().transform(response -> {
                    try {
                        return objectMapper.readValue(response.body(), RdapData.class);
                    } catch (Exception e) {
                        throw new RuntimeException("RDAP Mapping error for IP: " + ip, e);
                    }
                })
                .onFailure().transform(failure ->
                        new RuntimeException("RdapService failed after retries for IP: " + ip, failure)
                );
    }

    private void logResponse(HttpRequest request, HttpResponse<String> response) {
        log.info("[{}] {} | ResponseCode: {}", request.method(), request.uri(), response.statusCode());
    }
}
