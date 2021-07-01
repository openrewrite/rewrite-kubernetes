package org.openrewrite.kubernetes;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.kubernetes.util.ResourceValue;
import org.openrewrite.yaml.MergeValueVisitor;
import org.openrewrite.yaml.XPathMatcher;

@Value
@EqualsAndHashCode(callSuper = true)
public class CapResourceValueToMaximum extends Recipe {

    @Option(displayName = "Resource value type",
            description = "The type of resource to search for.",
            example = "limits",
            valid = {"limits", "requests"})
    String resourceValueType;
    @Option(displayName = "Resource type",
            description = "The type of resource value to search for.",
            example = "memory")
    String resourceType;
    @Option(displayName = "Resource limit",
            description = "The resource maximum to search for to find resources that request more than the maximum.",
            example = "2Gi")
    String resourceLimit;

    @Override
    public String getDisplayName() {
        return "Cap exceeds resource value";
    }

    @Override
    public String getDescription() {
        return "Cap resource values that exceed a specific maximum.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        ResourceValue max = ResourceValue.parseResourceString(resourceLimit);

        return new MergeValueVisitor<>(
                "spec/containers/resources/" + resourceValueType + "/" + resourceType,
                new ExceedsResourceValue(max),
                (s, ctx) -> {
                    ResourceValue rv = ResourceValue.parseResourceString(s.getValue());
                    return s.withValue(max.convertTo(rv.getUnit()).toString());
                });
    }

}
