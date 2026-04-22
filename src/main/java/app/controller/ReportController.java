package app.controller;

import app.service.report.ReportService;
import app.util.NetAddressUtil;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/report")
@Authenticated
public class ReportController {
    @Inject
    Template report;

    @Inject
    Template validateError;

    @Inject
    ReportService reportService;

    @Inject
    CurrentVertxRequest currentVertxRequest;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance showReport(@QueryParam("address") String address) {
        logIncomingRequest();
        address = NetAddressUtil.cleanAddress(address);
        if (NetAddressUtil.isIp(address) || NetAddressUtil.isDomain(address)) {
            return report.data("address", address);
        } else {
            return validateError.data("request", currentVertxRequest.getCurrent().request());
        }
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<OutboundSseEvent> streamData(@QueryParam("address") String address, @Context Sse sse) {
        logIncomingRequest();

        return createSseMulti(sse,
                reportService.whoIsLikeReport(address),
                reportService.securityReport(address)
        );
    }

    private Multi<OutboundSseEvent> createSseMulti(Sse sse, Uni<String> whoIsUni, Uni<String> secUni) {
        return Multi.createBy().merging().streams(
                whoIsUni.onItem().transform(data -> sse.newEventBuilder().name("whoIs").data(data).build()).toMulti(),
                secUni.onItem().transform(data -> sse.newEventBuilder().name("security").data(data).build()).toMulti()
        );
    }

    private void logIncomingRequest() {
        var request = currentVertxRequest.getCurrent().request();

        String fullPath = request.path() + (request.query() != null ? "?" + request.query() : "");

        log.info("[{}] {}", request.method(), fullPath);
    }
}
