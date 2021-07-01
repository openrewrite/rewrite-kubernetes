package org.openrewrite.kubernetes;

import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.kubernetes.util.ResourceValue;
import org.openrewrite.yaml.tree.Yaml;

import java.util.function.BiPredicate;

@Value
public class ExceedsResourceValue implements BiPredicate<Yaml.Scalar, ExecutionContext> {
    ResourceValue maximum;

    @Override
    public boolean test(Yaml.Scalar scalar, ExecutionContext executionContext) {
        ResourceValue rv = ResourceValue.parseResourceString(scalar.getValue());
        return rv.getAbsoluteValue() > maximum.getAbsoluteValue();
    }
}
