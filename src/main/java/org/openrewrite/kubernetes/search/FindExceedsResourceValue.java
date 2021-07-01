package org.openrewrite.kubernetes.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.kubernetes.ExceedsResourceValue;
import org.openrewrite.kubernetes.util.ResourceValue;
import org.openrewrite.yaml.MergeValueVisitor;
import org.openrewrite.yaml.search.YamlSearchResult;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindExceedsResourceValue extends Recipe {

    @Option(displayName = "Resource value type",
            description = "The type of resource value to search for.",
            example = "limits",
            valid = {"limits", "requests"})
    String resourceValueType;
    @Option(displayName = "Resource limit type",
            description = "The type of resource limit to search for.",
            example = "memory")
    String resourceType;
    @Option(displayName = "Resource limit",
            description = "The resource limit maximum to search for to find resources that request more than the maximum.",
            example = "2Gi")
    String resourceLimit;

    @Override
    public String getDisplayName() {
        return "Find exceeds resource limit";
    }

    @Override
    public String getDescription() {
        return "Find resource manifests that have limits set beyond a specific maximum.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        ResourceValue max = ResourceValue.parseResourceString(resourceLimit);

        return new MergeValueVisitor<>(
                "spec/containers/resources/" + resourceValueType + "/" + resourceType,
                new ExceedsResourceValue(max),
                (s, ctx) -> s.withMarkers(s.getMarkers().addIfAbsent(new YamlSearchResult(Tree.randomId(),
                        FindExceedsResourceValue.this,
                        "exceeds maximum of " + max))));
    }

}
