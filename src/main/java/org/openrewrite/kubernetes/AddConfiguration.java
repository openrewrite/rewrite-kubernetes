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
package org.openrewrite.kubernetes;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kubernetes.tree.KubernetesModel;
import org.openrewrite.yaml.MergeYaml;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddConfiguration extends Recipe {

    @Option(displayName = "API version",
            description = "The Kubernetes resource API version to use.",
            example = "policy/v1beta1",
            required = false)
    @Nullable
    String apiVersion;

    @Option(displayName = "Resource kind",
            description = "The Kubernetes resource type the configured is required on.",
            example = "PodSecurityPolicy")
    String resourceKind;

    @Option(displayName = "Configuration path",
            description = "A JsonPath expression to locate Kubernetes configuration. Must be an absolute path.",
            example = "$.spec")
    String configurationPath;

    @Option(displayName = "Value",
            description = "The configuration that is added when necessary, including the key.",
            example = "privileged: false")
    @Language("yml")
    String value;

    @Override
    public Validated validate() {
        return super.validate().and(
                Validated.test("configurationPath",
                        "Configuration path must be absolute (i.e. must start with $).",
                        configurationPath,
                        p -> p.startsWith("$"))
        );
    }

    @Override
    public String getDisplayName() {
        return "Add Kubernetes configuration";
    }

    @Override
    public String getDescription() {
        return "Add default required configuration when it is missing.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new KubernetesVisitor<ExecutionContext>() {
            @Override
            public Yaml visitDocument(Yaml.Document document, ExecutionContext ctx) {

                KubernetesModel model = getKubernetesModel();
                if (!resourceKind.equals(model.getKind())) {
                    return document;
                }

                if (apiVersion != null && !apiVersion.equals(model.getApiVersion())) {
                    return document;
                }

                doAfterVisit(new MergeYaml(configurationPath, value, true, null));

                return document;
            }
        };
    }
}
