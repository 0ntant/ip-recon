package app.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@RegisterForReflection
@AllArgsConstructor
@Getter
public class NslookupReport {
    private Map<String, List<String>> addressIdDns;
    private String report;
}
