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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kubernetes.ContainerImage;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import static org.openrewrite.kubernetes.tree.K8S.Containers.inContainerSpec;
import static org.openrewrite.kubernetes.tree.K8S.Containers.isImageName;
import static org.openrewrite.kubernetes.tree.K8S.InitContainers.inInitContainerSpec;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindMissingDigest extends Recipe {

    @Option(displayName = "Include initContainers",
            description = "Boolean to indicate whether or not to treat initContainers/image identically to " +
                    "containers/image.",
            example = "false",
            required = false)
    boolean includeInitContainers;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/pod-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Find missing image digest";
    }

    @Override
    public String getDescription() {
        return "Find instances of a container name that fails to specify a digest.";
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
        String result = "missing digest";

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, ExecutionContext ctx) {
                Cursor c = getCursor();
                if ((inContainerSpec(c) || (includeInitContainers && inInitContainerSpec(c))) && isImageName(c)) {
                    ContainerImage image = new ContainerImage(scalar.getValue());
                    if (!image.getImageName().hasDigest()) {
                        return scalar.withMarkers(scalar.getMarkers().searchResult(result));
                    }
                }
                return super.visitScalar(scalar, ctx);
            }
        };
    }

}
