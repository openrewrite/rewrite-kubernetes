package org.openrewrite.kubernetes;

import org.openrewrite.kubernetes.tree.KubernetesModel;
import org.openrewrite.yaml.YamlVisitor;

@SuppressWarnings("NotNullFieldNotInitialized")
public class KubernetesVisitor<P> extends YamlVisitor<P> {
    protected KubernetesModel model;

    public Kubernetes.ResourceDocument visitKubernetesResource(Kubernetes.ResourceDocument resource, P p) {
        this.model = resource.getModel();
        return (Kubernetes.ResourceDocument) super.visitDocument(resource, p);
    }
}
