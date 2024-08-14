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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.kubernetes.ContainerImage;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.openrewrite.kubernetes.tree.K8S.Containers.inContainerSpec;
import static org.openrewrite.kubernetes.tree.K8S.Containers.isImageName;
import static org.openrewrite.kubernetes.tree.K8S.InitContainers.inInitContainerSpec;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindDisallowedImageTags extends Recipe {

    @Option(displayName = "Disallowed tags",
            description = "The set of image tags to find which are considered disallowed. This is a comma-separated list of tags.",
            example = "latest")
    String disallowedTags;

    @Option(displayName = "Include initContainers",
            description = "Boolean to indicate whether or not to treat initContainers/image identically to containers/image.",
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
        return "Find disallowed image tags";
    }

    @Override
    public String getDescription() {
        return "The set of image tags to find which are considered disallowed.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        List<String> disallowed = Arrays.asList(disallowedTags.split("\\s*,\\s*"));

        YamlIsoVisitor<ExecutionContext> visitor = new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, ExecutionContext ctx) {
                Yaml.Scalar s = super.visitScalar(scalar, ctx);
                Cursor c = getCursor();
                if ((inContainerSpec(c) || (includeInitContainers && inInitContainerSpec(c))) && isImageName(c)) {
                    ContainerImage image = new ContainerImage(scalar);
                    List<String> foundDisallowed = disallowed.stream().filter(t -> t.equals(image.getImageName().getTag())).collect(Collectors.toList());
                    if (!foundDisallowed.isEmpty()) {
                        s = SearchResult.found(s, "disallowed tag: [" + String.join(", ", foundDisallowed) + "]");
                    }
                }
                return s;
            }
        };
        return fileMatcher != null ? Preconditions.check(new FindSourceFiles(fileMatcher), visitor) : visitor;
    }

}
