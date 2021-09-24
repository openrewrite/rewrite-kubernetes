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
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import static org.openrewrite.kubernetes.tree.K8S.ResourceLimits.inResources;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindExceedsResourceRatio extends Recipe {

    @Option(displayName = "Resource limit type",
            description = "The type of resource limit to search for.",
            example = "memory",
            valid = {"cpu", "memory"})
    String resourceType;

    @Option(displayName = "Resource ratio",
            description = "The maximum ratio allowed between requests and limits.",
            example = "2")
    String ratioLimit;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/pod-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Find exceeds resource ratio";
    }

    @Override
    public String getDescription() {
        return "Find resource manifests that have requests to limits ratios beyond a specific maximum.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        String result = "exceeds max " + resourceType + " limits/requests ratio of " + ratioLimit;
        int resourceLimit = Integer.parseInt(this.ratioLimit);

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext executionContext) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, executionContext);
                Cursor c = getCursor();
                if (inResources(c)) {
                    JsonPathMatcher requestsMatcher = new JsonPathMatcher(".requests." + resourceType);
                    JsonPathMatcher limitsMatcher = new JsonPathMatcher(".limits." + resourceType);

                    return requestsMatcher.find(c)
                            .flatMap(req -> limitsMatcher.find(c)
                                    .map(lim -> {
                                        String reqValStr = valueFromEntry(req);
                                        if (reqValStr == null) {
                                            return e;
                                        }
                                        ResourceLimit reqLimit = new ResourceLimit(reqValStr);

                                        String limValStr = valueFromEntry(lim);
                                        if (limValStr == null) {
                                            return e;
                                        }
                                        ResourceLimit limLimit = new ResourceLimit(limValStr);

                                        if (reqLimit.exceedsRatio(resourceLimit, limLimit.getValue())) {
                                            return e.withMarkers(e.getMarkers().searchResult(result));
                                        }

                                        return e;
                                    }))
                            .orElse(e);
                }

                return e;
            }

        };
    }

    private static @Nullable String valueFromEntry(Object o) {
        if (!(o instanceof Yaml.Mapping.Entry)) {
            return null;
        }
        Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) o;
        if (!(e.getValue() instanceof Yaml.Scalar)) {
            return null;
        }
        Yaml.Scalar s = (Yaml.Scalar) e.getValue();
        return s.getValue();
    }
}
