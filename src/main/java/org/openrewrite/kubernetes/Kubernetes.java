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

import lombok.Getter;
import org.openrewrite.kubernetes.tree.KubernetesResource;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;
import java.util.stream.Collectors;

public class Kubernetes extends Yaml.Documents {
    public Kubernetes(Yaml.Documents documents) {
        super(
                documents.getId(),
                documents.getMarkers(),
                documents.getSourcePath(),
                documents.getDocuments()
        );
    }

    @Override
    public List<ResourceDocument> getDocuments() {
        return super.getDocuments().stream()
                .map(ResourceDocument::new)
                .collect(Collectors.toList());
    }

    public static class ResourceDocument extends Yaml.Document {
        @Getter
        private final KubernetesResource resource;

        public ResourceDocument(Yaml.Document document) {
            super(
                    document.getId(),
                    document.getPrefix(),
                    document.getMarkers(),
                    document.isExplicit(),
                    document.getBlocks(),
                    document.getEnd()
            );

            //noinspection ConstantConditions
            resource = document.getMarkers()
                    .findFirst(KubernetesResource.class)
                    .orElse(null);

            assert resource != null;
        }
    }
}
