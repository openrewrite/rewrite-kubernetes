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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kubernetes.ContainerImage;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.search.YamlSearchResult;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindImage extends Recipe {

    @Option(displayName = "Repository",
            description = "The repository part of the image name to search for in containers and initContainers.",
            example = "gcr.io",
            required = false)
    @Nullable
    String repository;

    @Option(displayName = "Image name",
            description = "The image name to search for in containers and initContainers.",
            example = "nginx")
    String imageName;

    @Option(displayName = "Image tag",
            description = "The tag part of the image name to search for in containers and initContainers.",
            example = "v1.2.3",
            required = false)
    @Nullable
    String imageTag;

    @Option(displayName = "Image digest",
            description = "The digest part of the image name to search for in containers and initContainers.",
            example = "95743679f67454e4f25c287c5c098120b55b9596",
            required = false)
    @Nullable
    String imageDigest;

    @Override
    public String getDisplayName() {
        return "Image name";
    }

    @Override
    public String getDescription() {
        return "The image name to search for in containers and initContainers.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        ContainerImage.ImageName imageToSearch = new ContainerImage.ImageName(repository, imageName, imageTag, imageDigest);
        YamlSearchResult result = new YamlSearchResult(this, imageToSearch.toString());

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, ExecutionContext executionContext) {
                if (ContainerImage.matches(getCursor())) {
                    ContainerImage image = new ContainerImage(scalar);
                    if (matches(image.getImageName(), imageToSearch)) {
                        return scalar.withMarkers(scalar.getMarkers().addIfAbsent(result));
                    }
                }
                return super.visitScalar(scalar, executionContext);
            }
        };
    }

    private static boolean matches(ContainerImage.ImageName imageName,
                                   ContainerImage.ImageName imageToSearch) {
        boolean matchesRepo =
                bothNull(imageName.getRepository(), imageToSearch.getRepository())
                        || firstContainsSecond(imageName.getRepository(), imageToSearch.getRepository())
                        || isGlobPattern(imageToSearch.getRepository());
        boolean matchesImage =
                bothNull(imageName.getImage(), imageToSearch.getImage())
                        || firstContainsSecond(imageName.getImage(), imageToSearch.getImage())
                        || isGlobPattern(imageToSearch.getImage());
        boolean matchesTag =
                bothNull(imageName.getTag(), imageToSearch.getTag())
                        || firstContainsSecond(imageName.getTag(), imageToSearch.getTag())
                        || isGlobPattern(imageToSearch.getTag());
        boolean matchesDigest =
                bothNull(imageName.getDigest(), imageToSearch.getDigest())
                        || firstContainsSecond(imageName.getDigest(), imageToSearch.getDigest())
                        || isGlobPattern(imageToSearch.getDigest());

        return matchesRepo && matchesImage && matchesTag && matchesDigest;
    }

    private static boolean bothNull(@Nullable String s1, @Nullable String s2) {
        return s1 == null && s2 == null;
    }

    private static boolean firstContainsSecond(@Nullable String s1, @Nullable String s2) {
        return s1 != null && s2 != null && s1.contains(s2);
    }

    private static boolean isGlobPattern(@Nullable String s) {
        return s != null && s.equals("*");
    }

}
