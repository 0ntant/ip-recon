package app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class RdapData {
    public String handle;
    public String startAddress;
    public String endAddress;
    public String ipVersion;
    public String name;
    public String country;
    public List<Event> events;

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {
        public String eventAction;
        public OffsetDateTime eventDate;
    }

    public String getRegistrationDate() {
        OffsetDateTime date = findDate("registration");
        return date != null ? formateDate(date) : null;
    }

    public String getLastChangedDate() {
        OffsetDateTime date = findDate("last changed");
        return date != null ? formateDate(date) : null;
    }

    private OffsetDateTime findDate(String action) {
        if (events == null) return null;
        return events.stream()
                .filter(e -> action.equalsIgnoreCase(e.eventAction))
                .map(e -> e.eventDate)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String formateDate(OffsetDateTime date) {
        if (date == null) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        return date.format(formatter);
    }
}
