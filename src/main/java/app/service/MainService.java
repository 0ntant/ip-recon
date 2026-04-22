package app.service;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;

@Slf4j
@QuarkusMain
@ApplicationScoped
public class MainService {

    @Inject
    SlackService slackService;

    @Inject
    ManagedExecutor executor;

    void onStart(@Observes StartupEvent ev) {
        log.info("Quarkus started, launching Slack...");
        executor.submit(() -> {
            try {
                slackService.startService();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String... args) {
        Quarkus.run(args);
    }
}
