/*
 * Copyright 2024 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.yaml.ChangePropertyValue;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeApiVersion extends Recipe {

    @Option(displayName = "Old API version",
            description = "The old Kubernetes API version to match.",
            example = "flowcontrol.apiserver.k8s.io/v1beta3")
    String oldApiVersion;

    @Option(displayName = "New API version",
            description = "The new Kubernetes API version to change to.",
            example = "flowcontrol.apiserver.k8s.io/v1")
    String newApiVersion;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            example = "**/pod-*.yml",
            required = false)
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Change Kubernetes API version";
    }

    @Override
    public String getDescription() {
        return "Change the Kubernetes API version in a resource.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangePropertyValue(
                "apiVersion",
                newApiVersion,
                oldApiVersion,
                null,
                null,
                fileMatcher).getVisitor();
    }
}
