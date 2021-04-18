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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.kubernetes.Kubernetes;
import org.openrewrite.kubernetes.KubernetesVisitor;
import org.openrewrite.marker.RecipeSearchResult;
import org.openrewrite.yaml.search.FindKey;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindResourceMissingConfiguration extends Recipe {

    @Option(displayName = "Resource kind",
            description = "The Kubernetes resource type to search on.",
            example = "Pod")
    String resourceKind;

    @Option(displayName = "Configuration path",
            description = "An XPath expression to locate Kubernetes configuration.",
            example = "/spec/containers/livenessProbe")
    String configurationPath;

    @Override
    public String getDisplayName() {
        return "Missing configuration";
    }

    @Override
    public String getDescription() {
        return "Find Kubernetes resources with missing configuration.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new KubernetesVisitor<ExecutionContext>() {
            @Override
            public Kubernetes.ResourceDocument visitKubernetesResource(Kubernetes.ResourceDocument resource, ExecutionContext executionContext) {
                return resourceKind.equals(resource.getModel().getKind()) && FindKey.find(resource, configurationPath).isEmpty() ?
                        resource.withMarker(new RecipeSearchResult(FindResourceMissingConfiguration.this)) :
                        resource;
            }

        };
    }
}
