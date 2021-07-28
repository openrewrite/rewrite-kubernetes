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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kubernetes.ContainerImage;
import org.openrewrite.marker.RecipeSearchResult;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.search.YamlSearchResult;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.kubernetes.tree.K8S.Containers.inContainerSpec;
import static org.openrewrite.kubernetes.tree.K8S.Containers.isImageName;
import static org.openrewrite.kubernetes.tree.K8S.InitContainers.inInitContainerSpec;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindDisallowedImageTags extends Recipe {

    @Option(displayName = "Disallowed tags",
            description = "The set of image tags to find which are considered disallowed.",
            example = "latest")
    Set<String> disallowedTags;

    @Option(displayName = "Include initContainers",
            description = "Boolean to indicate whether or not to treat initContainers/image identically to " +
                    "containers/image.",
            example = "false",
            required = false)
    boolean includeInitContainers;

    @Option(displayName = "Source path include filters",
            description = "Comma-separated list of glob patterns to use as filters to determine whether this Recipe " +
                    "should apply to a given file.",
            example = "**/*.yaml",
            required = false)
    @Nullable
    Set<String> sourcePathIncludeFilters;

    @Override
    public String getDisplayName() {
        return "Disallowed tags";
    }

    @Override
    public String getDescription() {
        return "The set of image tags to find which are considered disallowed.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        RecipeSearchResult applicableResult = new RecipeSearchResult(randomId(), this, "applicable");
        List<PathMatcher> matchers = (sourcePathIncludeFilters == null ? null : sourcePathIncludeFilters.stream()
                .map(s -> FileSystems.getDefault().getPathMatcher("glob:" + s))
                .collect(Collectors.toList()));

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext executionContext) {
                if (matchers != null) {
                    if (matchers.stream().noneMatch(m -> m.matches(documents.getSourcePath()))) {
                        return documents;
                    }
                }
                return documents.withMarkers(documents.getMarkers().addIfAbsent(applicableResult));
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        YamlSearchResult result = new YamlSearchResult(this, "disallowed tag: " + disallowedTags);
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, ExecutionContext ctx) {
                Cursor c = getCursor();
                if ((inContainerSpec(c) || (includeInitContainers && inInitContainerSpec(c))) && isImageName(c)) {
                    ContainerImage image = new ContainerImage(scalar);
                    if (disallowedTags.stream().anyMatch(t -> t.equals(image.getImageName().getTag()))) {
                        return scalar.withMarkers(scalar.getMarkers().addIfAbsent(result));
                    }
                }
                return super.visitScalar(scalar, ctx);
            }
        };
    }

}
