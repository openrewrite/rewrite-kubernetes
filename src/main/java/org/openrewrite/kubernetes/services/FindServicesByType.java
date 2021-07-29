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

package org.openrewrite.kubernetes.services;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kubernetes.search.EntryMarkingVisitor;
import org.openrewrite.kubernetes.tree.K8S;
import org.openrewrite.yaml.search.YamlSearchResult;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindServicesByType extends Recipe {

    @Option(displayName = "Service type",
            description = "Type of Kubernetes Service to find.",
            example = "NodePort",
            valid = {"ClusterIP", "NodePort", "LoadBalancer", "ExternalName"})
    String serviceType;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/pod-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Service type";
    }

    @Override
    public String getDescription() {
        return "Type of Kubernetes Service to find.";
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
        YamlSearchResult result = new YamlSearchResult(this, "type:" + serviceType);

        return new EntryMarkingVisitor() {
            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                Cursor c = getCursor();
                if (K8S.Service.isServiceSpec(c)) {
                    K8S.Service svc = K8S.asService(mapping);
                    if (serviceType.equals(svc.getType())) {
                        c.getParentOrThrow().putMessageOnFirstEnclosing(Yaml.Mapping.Entry.class, MARKER, result);
                        return mapping;
                    }
                }
                return super.visitMapping(mapping, ctx);
            }
        };
    }

}
