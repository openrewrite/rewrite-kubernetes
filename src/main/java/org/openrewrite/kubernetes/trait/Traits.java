package org.openrewrite.kubernetes.trait;

import org.openrewrite.internal.lang.Nullable;

public class Traits {

    private Traits() {
    }

    public static KubernetesResource.Matcher kubernetesResource(@Nullable String kind) {
        return new KubernetesResource.Matcher(kind);
    }
}
