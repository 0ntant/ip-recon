package app.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;

@RegisterForReflection
@AllArgsConstructor
@Getter
public class AttackTypeTitle {
    String en;
    String ru;
}
