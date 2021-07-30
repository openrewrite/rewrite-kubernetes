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

package org.openrewrite.kubernetes.rbac;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kubernetes.tree.K8S;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.openrewrite.internal.ListUtils.concat;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddRuleToRole extends Recipe {

    @Option(displayName = "RBAC Resource type",
            description = "Type of RBAC resource to which this recipe adds a rule.",
            example = "ClusterRole",
            valid = {"ClusterRole", "Role"})
    String rbacResourceType;

    @Option(displayName = "RBAC Resource name",
            description = "Glob pattern of the name of the RBAC resource to which this recipe adds a rule.",
            example = "my-cluster-role")
    String rbacResourceName;

    @Option(displayName = "API groups",
            description = "Comma-separated list of API groups to which this rule refers.",
            example = ",v1")
    Set<String> apiGroups;

    @Option(displayName = "Resource types",
            description = "Comma-separated list of Kubernetes resource types to which this rule refers.",
            example = "pods")
    Set<String> resources;

    @Option(displayName = "Resource names",
            description = "Comma-separated list of names of Kubernetes resources to which this rule applies.",
            example = "my-pod",
            required = false)
    @Nullable
    Set<String> resourceNames;

    @Option(displayName = "API verbs",
            description = "The API verbs to enable with this rule.",
            example = "get,list")
    Set<String> verbs;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/pod-*.yml")
    @Nullable
    String fileMatcher;

    @Nullable
    Yaml.Sequence.Entry newSequenceEntry;

    public AddRuleToRole(String rbacResourceType,
                         String rbacResourceName,
                         Set<String> apiGroups,
                         Set<String> resources,
                         @Nullable Set<String> resourceNames,
                         Set<String> verbs,
                         @Nullable String fileMatcher) {
        this.rbacResourceType = rbacResourceType;
        this.rbacResourceName = rbacResourceName;
        this.apiGroups = apiGroups;
        this.resources = resources;
        this.resourceNames = resourceNames;
        this.verbs = verbs;
        this.fileMatcher = fileMatcher;

        String apiGroupsStr = setToString(apiGroups);
        String resourcesStr = setToString(resources);
        String resourceNamesStr = setToString(resourceNames);
        String verbsStr = setToString(verbs);
        List<Yaml.Documents> docs = new YamlParser().parse("- apiGroups: " + apiGroupsStr + "\n"
                + "  resources: " + resourcesStr + "\n"
                + (resourceNamesStr != null ? "  resourceNames: " + resourceNamesStr + "\n" : "")
                + "  verbs: " + verbsStr);
        this.newSequenceEntry =
                ((Yaml.Sequence) docs.get(0).getDocuments().get(0).getBlock()).getEntries().get(0).withPrefix("\n");
    }

    @Override
    public String getDisplayName() {
        return "Add RBAC rules";
    }

    @Override
    public String getDescription() {
        return "Add RBAC rules to ClusterRoles or namespaced Roles.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        } else {
            return null;
        }
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        PathMatcher globMatcher = FileSystems.getDefault().getPathMatcher("glob:" + rbacResourceName);

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext executionContext) {
                Cursor c = getCursor();
                if (K8S.inKind(rbacResourceType, c) && K8S.Metadata.isMetadata(c)) {
                    K8S.Metadata meta = K8S.asMetadata((Yaml.Mapping) entry.getValue());
                    if (globMatcher.matches(Paths.get(meta.getName()))) {
                        c.putMessageOnFirstEnclosing(Yaml.Document.class, "resource-name", meta.getName());
                    }
                }
                return super.visitMappingEntry(entry, executionContext);
            }

            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext executionContext) {
                Yaml.Sequence s = super.visitSequence(sequence, executionContext);
                Cursor c = getCursor();
                if (K8S.RBAC.inRules(c) && c.getNearestMessage("resource-name") != null) {
                    if (!s.getEntries().contains(newSequenceEntry)) {
                        return s.withEntries(concat(s.getEntries(), newSequenceEntry));
                    }
                }
                return s;
            }
        };
    }

    private static @Nullable String setToString(@Nullable Set<String> strs) {
        if (strs == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean needsComma = false;
        for (String s : strs) {
            if (needsComma) {
                sb.append(", ");
            } else {
                needsComma = true;
            }
            sb.append("\"").append(s).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

}
