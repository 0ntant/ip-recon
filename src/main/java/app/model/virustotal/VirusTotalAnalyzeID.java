package app.model.virustotal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class VirusTotalAnalyzeID {
    @JsonProperty("data")
    private Data data;

    @Getter
    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String id;
        private String type;
    }

    public String getId() {
        return data.getId();
    }
}
