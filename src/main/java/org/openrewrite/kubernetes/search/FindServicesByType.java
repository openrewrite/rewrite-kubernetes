/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.kubernetes.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.kubernetes.Kubernetes;
import org.openrewrite.yaml.XPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.search.FindKey;
import org.openrewrite.yaml.search.YamlSearchResult;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Set;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindServicesByType extends Recipe {

    @Option(displayName = "Service type",
            description = "Type of Kubernetes Service to find.",
            example = "NodePort",
            valid = {"ClusterIP", "NodePort", "LoadBalancer", "ExternalName"})
    String serviceType;

    @Override
    public String getDisplayName() {
        return "Service type";
    }

    @Override
    public String getDescription() {
        return "Type of Kubernetes Service to find.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher matcher = new XPathMatcher("/spec/type");
        YamlSearchResult result = new YamlSearchResult(this, "type:" + serviceType);

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                Kubernetes.ResourceDocument r = (Kubernetes.ResourceDocument) document;
                if (!"Service".equals((r.getModel().getKind()))) {
                    return document;
                }

                Set<Yaml.Mapping.Entry> typeEntries = FindKey.find(document, "/spec/type");
                if (typeEntries.isEmpty() && "ClusterIP".equals(serviceType) || typeEntries.stream()
                        .anyMatch(e -> e.getValue() instanceof Yaml.Scalar && serviceType.equals(((Yaml.Scalar) e.getValue()).getValue()))) {
                    return document
                            .withMarkers(document.getMarkers().addIfAbsent(result))
                            .withBlock((Yaml.Block) requireNonNull(visit(document.getBlock(), ctx, getCursor())));
                }
                return document;
            }
        };
    }

}
