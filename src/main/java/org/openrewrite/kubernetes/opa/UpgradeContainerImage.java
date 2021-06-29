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
package org.openrewrite.kubernetes.opa;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.XPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Optional;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradeContainerImage extends Recipe {

    @Option(displayName = "Allowed repos",
            description = "Comma-separated list of allowed repository names.",
            example = "repo.dev.lan,repo.prod.wan")
    Set<String> allowedRepos;

    @Option(displayName = "Preferred repo",
            description = "Name of the preferred repository to update an image reference to if it's not one of the " +
                    "approved repos.",
            example = "repo.prod.wan")
    String preferredRepo;

    @Option(displayName = "Image name",
            description = "Name of the container image to upgrade the version tag and optionally the repository to.",
            example = "nginx",
            required = false)
    @Nullable
    String imageName;

    @Option(displayName = "Image version tag",
            description = "Version tag of the container to use when upgrading the image.",
            example = "latest",
            required = false)
    @Nullable
    String imageTag;

    @Override
    public String getDisplayName() {
        return "Change image name";
    }

    @Override
    public String getDescription() {
        return "Change container image references throughout YAML manifests.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher workloadMatcher = new XPathMatcher("/spec/template/spec/containers/image");
        XPathMatcher podMatcher = new XPathMatcher("/spec/containers/image");

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext executionContext) {
                if (workloadMatcher.matches(getCursor()) || podMatcher.matches(getCursor())) {

                    return getScalarValue(entry)
                            .map(s -> {
                                String currentImageName = s.getValue();
                                if (null != imageName && !currentImageName.contains(imageName)) {
                                    return entry;
                                }

                                int slashIdx = currentImageName.indexOf('/');
                                String currentRepoName = slashIdx > -1 ? currentImageName.substring(0, slashIdx) : "";
                                boolean hasAllowedRepo = allowedRepos.contains(currentRepoName);

                                int colonIdx = currentImageName.lastIndexOf(':');
                                String currentVersionTag = colonIdx > -1 ? currentImageName.substring(colonIdx + 1) : "";

                                String newImageName = (currentRepoName.isEmpty() || !hasAllowedRepo ? preferredRepo : currentRepoName)
                                        + "/"
                                        + (null != imageName ? imageName : currentImageName.substring(slashIdx > -1 ? slashIdx + 1 : 0, colonIdx))
                                        + ":"
                                        + (imageTag != null && !currentVersionTag.equals(imageTag) ? imageTag : currentVersionTag);
                                if (!newImageName.equals(currentImageName)) {
                                    return entry.withValue(s.withValue(newImageName));
                                } else {
                                    return entry;
                                }
                            })
                            .orElse(entry);
                }
                return super.visitMappingEntry(entry, executionContext);
            }
        };
    }

    private static Optional<Yaml.Scalar> getScalarValue(Yaml.Mapping.Entry entry) {
        if (entry.getValue() instanceof Yaml.Scalar) {
            return Optional.of(((Yaml.Scalar) entry.getValue()));
        }
        return Optional.empty();
    }

}
