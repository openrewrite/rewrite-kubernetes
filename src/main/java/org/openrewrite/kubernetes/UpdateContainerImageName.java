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

package org.openrewrite.kubernetes;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpdateContainerImageName extends Recipe {

    @Option(displayName = "Repository to find",
            description = "The repository part of the image name to search for in containers and initContainers.",
            example = "gcr.io",
            required = false)
    @Nullable
    String repoToFind;

    @Option(displayName = "Image name to find",
            description = "The image name to search for in containers and initContainers.",
            example = "nginx")
    String imageToFind;

    @Option(displayName = "Image tag to find",
            description = "The tag part of the image name to search for in containers and initContainers.",
            example = "v1.2.3",
            required = false)
    @Nullable
    String tagToFind;

    @Option(displayName = "Repository to update",
            description = "The repository part of the image name to update to in containers and initContainers.",
            example = "gcr.io/account/bucket",
            required = false)
    @Nullable
    String repoToUpdate;

    @Option(displayName = "Image name to update",
            description = "The image name to update to in containers and initContainers.",
            example = "nginx",
            required = false)
    @Nullable
    String imageToUpdate;

    @Option(displayName = "Image tag to update",
            description = "The tag part of the image name to update to in containers and initContainers.",
            example = "v1.2.3",
            required = false)
    @Nullable
    String tagToUpdate;

    @Option(displayName = "Include initContainers",
            description = "Boolean to indicate whether or not to treat initContainers/image identically to " +
                    "containers/image.",
            example = "false")
    boolean includeInitContainers;

    @Override
    public String getDisplayName() {
        return "Update image name";
    }

    @Override
    public String getDescription() {
        return "Search for image names that match patterns and replace the components of the name with new values.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        ContainerImage.ImageName imageToSearch = new ContainerImage.ImageName(repoToFind, imageToFind, tagToFind, "*");

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, ExecutionContext executionContext) {
                Yaml.Scalar s = super.visitScalar(scalar, executionContext);
                if (ContainerImage.matches(getCursor(), includeInitContainers)) {
                    ContainerImage image = new ContainerImage(s);
                    if (image.getImageName().matches(imageToSearch)) {
                        ContainerImage.ImageName newName = image.getImageName();
                        if (null != repoToUpdate) {
                            newName = newName.withRepository(repoToUpdate);
                        }
                        if (null != imageToUpdate) {
                            newName = newName.withImage(imageToUpdate);
                        }
                        if (null != tagToUpdate) {
                            newName = newName.withTag(tagToUpdate);
                        }
                        return s.withValue(newName.toString());
                    }
                }
                return s;
            }
        };
    }

}
