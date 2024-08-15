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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.kubernetes.KubernetesVisitor;
import org.openrewrite.kubernetes.tree.K8S;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.search.FindKey;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.openrewrite.internal.ListUtils.concat;

@Value
@EqualsAndHashCode(callSuper = false)
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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        KubernetesVisitor<ExecutionContext> visitor = new KubernetesVisitor<ExecutionContext>() {
            private final PathMatcher globMatcher = FileSystems.getDefault().getPathMatcher("glob:" + rbacResourceName);
            private final Yaml.Sequence.Entry newSequenceEntry = generateSequence();

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Cursor c = getCursor();
                if (K8S.inKind(rbacResourceType, c) && K8S.Metadata.isMetadata(c)) {
                    K8S.Metadata meta = K8S.asMetadata((Yaml.Mapping) entry.getValue());
                    Yaml.Document document = c.dropParentUntil(org.openrewrite.yaml.tree.Yaml.Document.class::isInstance).getValue();
                    if (globMatcher.matches(Paths.get(meta.getName())) && isSupportedAPI(document)) {
                        if (containsRule(FindKey.find(document, "$.rules[*].*"))) {
                            return entry;
                        }
                        Yaml.Block addToRules = ((Yaml.Mapping.Entry) (FindKey.find(document, "$.rules").toArray()[0])).getValue();
                        getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, "RULE_KEY", addToRules);
                    }
                }
                return (Yaml.Mapping.Entry) super.visitMappingEntry(entry, ctx);
            }

            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext ctx) {
                Object found = getCursor().getNearestMessage("RULE_KEY");
                if (found == sequence) {
                    sequence = sequence.withEntries(concat(sequence.getEntries(), newSequenceEntry));
                    maybeUpdateModel();
                }
                return (Yaml.Sequence) super.visitSequence(sequence, ctx);
            }

            private boolean isSupportedAPI(Yaml.Document document) {
                Set<Yaml> apiVersion = FindKey.find(document, "$.apiVersion");
                return apiVersion.size() == 1 &&
                        apiVersion.toArray()[0] instanceof Yaml.Mapping.Entry &&
                        ((Yaml.Mapping.Entry) apiVersion.toArray()[0]).getValue() instanceof Yaml.Scalar &&
                        "rbac.authorization.k8s.io/v1".equals(((Yaml.Scalar) ((Yaml.Mapping.Entry) apiVersion.toArray()[0]).getValue()).getValue());
            }

            private boolean containsRule(Set<Yaml> rules) {

                for (Yaml yaml : rules) {
                    Yaml.Mapping.Entry entry = (Yaml.Mapping.Entry) yaml;
                    Set<String> values = extractEntryValues(entry);
                    if ("apiGroups".equals(entry.getKey().getValue()) && (values.size() != apiGroups.size() || !values.containsAll(apiGroups))) {
                        return false;
                    }
                    if ("resources".equals(entry.getKey().getValue()) && (values.size() != resources.size() || !values.containsAll(resources))) {
                        return false;
                    }
                    if ("verbs".equals(entry.getKey().getValue()) && (values.size() != verbs.size() || !values.containsAll(verbs))) {
                        return false;
                    }

                    if (resourceNames != null) {
                        if ("resourceNames".equals(entry.getKey().getValue()) && (values.size() != resourceNames.size() || !values.containsAll(resourceNames))) {
                            return false;
                        }
                    }
                }
                return true;
            }

            private Set<String> extractEntryValues(Yaml.Mapping.@Nullable Entry entry) {
                Set<String> values = new HashSet<>();
                if (entry != null) {
                    if (entry.getValue() instanceof Yaml.Scalar) {
                        values.add(((Yaml.Scalar) entry.getValue()).getValue());
                    } else if (entry.getValue() instanceof Yaml.Sequence) {
                        for (Yaml.Sequence.Entry yaml : ((Yaml.Sequence) entry.getValue()).getEntries()) {
                            if (yaml.getBlock() instanceof Yaml.Scalar) {
                                values.add(((Yaml.Scalar) yaml.getBlock()).getValue());
                            }
                        }
                    }
                }
                return values;
            }
        };
        return fileMatcher != null ? Preconditions.check(new FindSourceFiles(fileMatcher), visitor) : visitor;
    }

    private @Nullable String setToString(@Nullable Set<String> strs) {
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

    private Yaml.Sequence.Entry generateSequence() {
        String apiGroupsStr = setToString(apiGroups);
        String resourcesStr = setToString(resources);
        String resourceNamesStr = setToString(resourceNames);
        String verbsStr = setToString(verbs);
        Stream<Yaml.Documents> docs = new YamlParser()
                .parse("- apiGroups: " + apiGroupsStr + "\n"
                + "  resources: " + resourcesStr + "\n"
                + (resourceNamesStr != null ? "  resourceNames: " + resourceNamesStr + "\n" : "")
                + "  verbs: " + verbsStr)
                .map(Yaml.Documents.class::cast);
        return ((Yaml.Sequence) docs.findFirst().get().getDocuments().get(0).getBlock()).getEntries().get(0).withPrefix("\n");
    }
}
