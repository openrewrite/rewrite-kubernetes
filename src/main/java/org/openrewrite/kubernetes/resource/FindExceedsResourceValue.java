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
package org.openrewrite.kubernetes.resource;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import static org.openrewrite.kubernetes.tree.K8S.ResourceLimits.inLimits;
import static org.openrewrite.kubernetes.tree.K8S.ResourceLimits.inRequests;
import static org.openrewrite.kubernetes.tree.K8S.asResourceLimits;

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
            example = "memory",
            valid = {"cpu", "memory"})
    String resourceType;

    @Option(displayName = "Resource limit",
            description = "The resource limit maximum to search for to find resources that request more than the maximum.",
            example = "2Gi")
    String resourceLimit;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/pod-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Find exceeds resource limit";
    }

    @Override
    public String getDescription() {
        return "Find resource manifests that have limits set beyond a specific maximum.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        ResourceLimit limit = new ResourceLimit(resourceLimit);
        String result = "exceeds maximum of " + limit.getValue();

        YamlIsoVisitor<ExecutionContext> visitor = new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, ExecutionContext ctx) {
                Cursor c = getCursor();
                if (((inLimits(resourceType, c) && "limits".equals(resourceValueType)) || (inRequests(resourceType, c) && "requests".equals(resourceValueType))) && asResourceLimits(scalar).getValue().exceeds(limit.getValue())) {
                    return SearchResult.found(scalar, result);
                }
                return super.visitScalar(scalar, ctx);
            }
        };
        return fileMatcher != null ? Preconditions.check(new HasSourcePath(fileMatcher), visitor) : visitor;
    }
}
