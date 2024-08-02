package org.openrewrite.kubernetes.trait;

public class Traits {

    private Traits() {
    }

    public static KubernetesResource.Matcher kubernetesResource() {
        return new KubernetesResource.Matcher();
    }
}
