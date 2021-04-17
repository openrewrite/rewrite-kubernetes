package org.openrewrite.kubernetes.tree;

import lombok.Value;
import org.openrewrite.marker.Marker;

import java.util.Map;

@Value
public class KubernetesResource implements Marker {
    String apiVersion;
    String kind;
    Metadata metadata;

    @Value
    public static class Metadata {
        String name;
        Map<String, String> annotations;
        Map<String, String> labels;
    }
}
