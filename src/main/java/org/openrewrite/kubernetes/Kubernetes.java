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
