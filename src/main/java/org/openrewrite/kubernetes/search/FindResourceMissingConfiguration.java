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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.kubernetes.trait.KubernetesResource;
import org.openrewrite.kubernetes.trait.Traits;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindResourceMissingConfiguration extends Recipe {

    @Option(displayName = "Resource kind",
            description = "The Kubernetes resource type to search on.",
            example = "Pod",
            required = false)
    @Nullable
    String resourceKind;

    @Option(displayName = "Configuration path",
            description = "A JsonPath expression to locate Kubernetes configuration.",
            example = "$.spec.containers.livenessProbe")
    String configurationPath;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/pod-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Find missing configuration";
    }

    @Override
    public String getDescription() {
        return "Find Kubernetes resources with missing configuration.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<? extends Tree, ExecutionContext> kubernetesResourceVisitor = Traits.kubernetesResource(resourceKind)
                .asVisitor((KubernetesResource resource, ExecutionContext ctx) -> {
                    AtomicBoolean pathFound = new AtomicBoolean(false);
                    new YamlIsoVisitor<AtomicBoolean>() {
                        @Override
                        public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, AtomicBoolean bool) {
                            if (new JsonPathMatcher(configurationPath).matches(getCursor())) {
                                bool.set(true);
                            }
                            return bool.get() ? entry : super.visitMappingEntry(entry, bool);
                        }
                    }.visitNonNull(resource.getTree(), pathFound, requireNonNull(resource.getCursor().getParent()));
                    return pathFound.get() ? resource.getTree() : SearchResult.found(resource.getTree(), "missing: " + configurationPath);
                });

        if (fileMatcher != null) {
            return Preconditions.check(new FindSourceFiles(fileMatcher), kubernetesResourceVisitor);
        }
        return kubernetesResourceVisitor;
    }
}
