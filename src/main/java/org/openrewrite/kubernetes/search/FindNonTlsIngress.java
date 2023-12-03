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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kubernetes.tree.K8S;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

public class FindNonTlsIngress extends Recipe {
    private static final SearchResult MISSING_TLS = new SearchResult(Tree.randomId(), "missing TLS");
    private static final SearchResult MISSING_DISALLOW_HTTP = new SearchResult(Tree.randomId(), "missing disallow http");

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/pod-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Find non-TLS Ingresses";
    }

    @Override
    public String getDescription() {
        return "Find Ingress resources that don't disallow HTTP or don't have TLS configured.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        YamlIsoVisitor<ExecutionContext> visitor = new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                if (K8S.inKind("Ingress", getCursor())) {
                    Yaml.Document d = super.visitDocument(document, ctx);
                    if (!K8S.Ingress.isTlsConfigured(getCursor())) {
                        d = d.withMarkers(d.getMarkers().addIfAbsent(MISSING_TLS));
                    }
                    if (!K8S.Ingress.isDisallowHttpConfigured(getCursor())) {
                        d = d.withMarkers(d.getMarkers().addIfAbsent(MISSING_DISALLOW_HTTP));
                    }
                    return d;
                }
                return super.visitDocument(document, ctx);
            }
        };
        return fileMatcher != null ? Preconditions.check(new FindSourceFiles(fileMatcher), visitor) : visitor;
    }
}
