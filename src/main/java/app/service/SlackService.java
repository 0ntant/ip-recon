package app.service;

import app.service.report.ReportService;
import app.util.NetAddressUtil;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.MessageEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Slf4j
public class SlackService {

    @ConfigProperty(name = "slack.xoxb")
    String botToken;

    @ConfigProperty(name = "slack.xapp")
    String appToken;

    @Inject
    ReportService reportService;

    public void startService() throws Exception {
        log.info("Start slack service");

        AppConfig config = new AppConfig();
        config.setSingleTeamBotToken(botToken);

        App app = new App(config);

        app.event(MessageEvent.class, (payload, ctx) -> {
            handleMessageEvent(payload.getEvent(), ctx);
            return ctx.ack();
        });

        new SocketModeApp(appToken, app).start();
    }

    private void handleMessageEvent(MessageEvent event, EventContext ctx) {
        try {
            if (isBotMessage(event)) {
                return;
            }

            String channel = event.getChannel();
            String text = event.getText();

            if (channel == null || text == null) {
                log.warn("Skip null event");
                return;
            }

            text = NetAddressUtil.cleanAddress(text);
            log.info("Slack request: {}", text);

            processReports(channel, text, ctx);

        } catch (Exception e) {
            log.error("Slack event processing error", e);
        }
    }

    private boolean isBotMessage(MessageEvent event) {
        return event.getBotId() != null;
    }

    private void processReports(String channel, String text, EventContext ctx) {

        reportService.securityReport(text)
                .subscribe().with(result ->
                        sendMessage(channel, result, ctx)
                );

        reportService.whoIsLikeReport(text)
                .subscribe().with(result ->
                        sendMessage(channel, result, ctx)
                );
    }

    private void sendMessage(String channel, String message, EventContext ctx) {
        try {
            log.info("Sending message to Slack channel");

            ctx.client().chatPostMessage(r -> r
                    .channel(channel)
                    .text(message)
            );

        } catch (Exception e) {
            log.error("Slack send error", e);
        }
    }
}
