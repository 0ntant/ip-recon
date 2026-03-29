package app.service;

import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.dns.DnsClient;
import io.vertx.core.dns.DnsClientOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class NslookupService {

    @Inject
    Vertx vertx;

    @CacheResult(cacheName = "nslookup-reports-cache")
    public Uni<List<String>> getRecordsAsync(String dnsAddress) {
        DnsClient client = vertx.createDnsClient(new DnsClientOptions()
                .setHost("1.1.1.1")
                .setPort(53));

        return client.resolveA(dnsAddress)
                .onFailure().transform(e ->
                        new RuntimeException("DNS lookup for %s failed".formatted(dnsAddress), e)
                );
    }
}
