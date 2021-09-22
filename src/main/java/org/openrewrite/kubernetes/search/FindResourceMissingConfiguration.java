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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kubernetes.tree.K8S;
import org.openrewrite.marker.RecipeSearchResult;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
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
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        RecipeSearchResult result = new RecipeSearchResult(Tree.randomId(), this);

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                Yaml.Block b = (Yaml.Block) visit(document.getBlock(), ctx, getCursor());
                boolean inKind = resourceKind == null || K8S.inKind(resourceKind, getCursor());
                if (inKind && !("true".equals(getCursor().getMessage(FindResourceMissingConfiguration.class.getSimpleName())))) {
                    return document.withBlock(b).withMarkers(document.getMarkers().addIfAbsent(result));
                }
                return document;
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                if (K8S.firstEnclosingEntryMatching(configurationPath, getCursor()).isPresent()) {
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class,
                            FindResourceMissingConfiguration.class.getSimpleName(), "true");
                }
                return super.visitMappingEntry(entry, ctx);
            }
        };
    }
}
