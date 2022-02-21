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
import org.openrewrite.yaml.search.FindKey;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
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
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        } else {
            return null;
        }
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<ExecutionContext>() {
            private final PathMatcher globMatcher = FileSystems.getDefault().getPathMatcher("glob:" + rbacResourceName);
            private final Yaml.Sequence.Entry newSequenceEntry = generateSequence();

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext executionContext) {
                Cursor c = getCursor();
                if (K8S.inKind(rbacResourceType, c) && K8S.Metadata.isMetadata(c)) {
                    K8S.Metadata meta = K8S.asMetadata((Yaml.Mapping) entry.getValue());
                    Yaml.Document document = c.dropParentUntil(p -> p instanceof Yaml.Document).getValue();
                    if (globMatcher.matches(Paths.get(meta.getName())) && isSupportedAPI(document)) {
                        Set<Yaml> resourceRules = FindKey.find(document, "$.rules[*]");
                        for (Yaml yaml : new ArrayList<>(resourceRules)) {
                            if (containsRule((Yaml.Mapping) yaml)) {
                                return entry;
                            }
                        }
                        Yaml.Block addToRules = ((Yaml.Mapping.Entry) (FindKey.find(document, "$.rules").toArray()[0])).getValue();
                        executionContext.putMessage("RULE_KEY", addToRules);
                    }
                }
                return super.visitMappingEntry(entry, executionContext);
            }

            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext executionContext) {
                Object found = executionContext.getMessage("RULE_KEY");
                if (found == sequence) {
                    sequence = sequence.withEntries(concat(sequence.getEntries(), newSequenceEntry));
                }
                return super.visitSequence(sequence, executionContext);
            }

            private boolean isSupportedAPI(Yaml.Document document) {
                Set<Yaml> apiVersion = FindKey.find(document, "$.apiVersion");
                return apiVersion.size() == 1 &&
                        apiVersion.toArray()[0] instanceof Yaml.Mapping.Entry &&
                        ((Yaml.Mapping.Entry) apiVersion.toArray()[0]).getValue() instanceof Yaml.Scalar &&
                        "rbac.authorization.k8s.io/v1".equals(((Yaml.Scalar) ((Yaml.Mapping.Entry) apiVersion.toArray()[0]).getValue()).getValue());
            }

            private boolean containsRule(Yaml.Mapping rules) {
                Set<Yaml> apiGroupsSet = FindKey.find(rules, "..rules.apiGroups");
                Set<String> apiGroupValues = extractEntryValues(apiGroupsSet);
                if (apiGroupValues.size() != apiGroups.size() || !apiGroupValues.containsAll(apiGroups)) {
                    return false;
                }

                Set<Yaml> apiResourcesSet = FindKey.find(rules, "..rules.resources");
                Set<String> resourceValues = extractEntryValues(apiResourcesSet);
                if (resourceValues.size() != resources.size() || !resourceValues.containsAll(resources)) {
                    return false;
                }

                Set<Yaml> apiVerbsSet = FindKey.find(rules, "..rules.verbs");
                Set<String> verbValues = extractEntryValues(apiVerbsSet);
                if (verbValues.size() != verbs.size() || !verbValues.containsAll(verbs)) {
                    return false;
                }

                if (resourceNames != null) {
                    Set<Yaml> resourceNamesSet = FindKey.find(rules, "..rules.resourceNames");
                    Set<String> resourceNamesValues = extractEntryValues(resourceNamesSet);
                    return (resourceNamesValues.size() != resourceNames.size() || !resourceNamesValues.containsAll(resourceNames));
                }

                return true;
            }

            private Set<String> extractEntryValues(@Nullable Set<Yaml> entries) {
                Set<String> values = new HashSet<>();
                if (entries != null && entries.size() == 1) {
                    Yaml.Mapping.Entry entry = ((Yaml.Mapping.Entry) entries.toArray()[0]);
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
        List<Yaml.Documents> docs = new YamlParser().parse("- apiGroups: " + apiGroupsStr + "\n"
                + "  resources: " + resourcesStr + "\n"
                + (resourceNamesStr != null ? "  resourceNames: " + resourceNamesStr + "\n" : "")
                + "  verbs: " + verbsStr);
        return ((Yaml.Sequence) docs.get(0).getDocuments().get(0).getBlock()).getEntries().get(0).withPrefix("\n");
    }
}
