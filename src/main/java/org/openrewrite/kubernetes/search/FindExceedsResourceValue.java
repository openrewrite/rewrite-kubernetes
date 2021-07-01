/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.kubernetes.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.kubernetes.util.ResourceValue;
import org.openrewrite.yaml.XPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.search.YamlSearchResult;
import org.openrewrite.yaml.tree.Yaml;

import static org.openrewrite.Tree.randomId;

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
        XPathMatcher resourceMatcher = new XPathMatcher("spec/containers/resources/" + resourceValueType + "/" + resourceType);

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext executionContext) {
                if (resourceMatcher.matches(getCursor()) && entry.getValue() instanceof Yaml.Scalar) {
                    Yaml.Scalar scalarValue = (Yaml.Scalar) entry.getValue();
                    ResourceValue rv = ResourceValue.parseResourceString(scalarValue.getValue());
                    if (rv.getAbsoluteValue() > max.getAbsoluteValue()) {
                        return entry.withValue(scalarValue.withMarkers(scalarValue.getMarkers().addIfAbsent(new YamlSearchResult(randomId(),
                                FindExceedsResourceValue.this,
                                "exceeds maximum of " + max))));
                    }
                }
                return super.visitMappingEntry(entry, executionContext);
            }
        };
    }

}
