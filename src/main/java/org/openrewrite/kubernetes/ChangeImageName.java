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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.XPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Optional;

import static org.openrewrite.internal.StringUtils.isNullOrEmpty;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeImageName extends Recipe {

    @Option(displayName = "Repository",
            description = "Name of the container repository to use when updating the image.",
            example = "repo.mycompany.com",
            required = false)
    @Nullable
    String repository;
    @Option(displayName = "Image name",
            description = "Name of the container image to use when updating.",
            example = "nginx")
    @NonNull
    String imageName;
    @Option(displayName = "Image tag",
            description = "Tag of the container to use when updating the image.",
            example = "latest")
    @NonNull
    String imageTag;

    @Override
    public String getDisplayName() {
        return "Change Image name";
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
                                String newImageName = (!isNullOrEmpty(repository) ? repository + "/" : "")
                                        + imageName
                                        + ":" + imageTag;
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
