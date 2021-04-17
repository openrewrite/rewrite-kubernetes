package org.openrewrite.kubernetes;

import org.openrewrite.kubernetes.tree.KubernetesResource;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RefreshModel<P> extends YamlIsoVisitor<P> {
    private static final Pattern METADATA_LABEL = Pattern.compile("/metadata/labels/(.+)");
    private static final Pattern METADATA_ANNOTATION = Pattern.compile("/metadata/annotations/(.+)");

    @Override
    public Yaml.Document visitDocument(Yaml.Document document, P p) {
        Yaml.Document d = super.visitDocument(document, p);

        KubernetesResource resource = new KubernetesResource(
                getCursor().getMessage("apiVersion"),
                getCursor().getMessage("kind"),
                new KubernetesResource.Metadata(
                        getCursor().getMessage("name"),
                        getCursor().getMessage("annotations"),
                        getCursor().getMessage("labels")
                )
        );

        return d.withMarkers(document.getMarkers().computeByType(resource, (old, n) -> n));
    }

    @Override
    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        String path = getPath();

        if (entry.getValue() instanceof Yaml.Scalar) {
            String value = ((Yaml.Scalar) entry.getValue()).getValue();

            switch (path) {
                case "/apiVersion":
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, "apiVersion", value);
                    break;
                case "/kind":
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, "kind", value);
                    break;
                case "/metadata/name":
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, "name", value);
                    break;
            }

            Matcher label = METADATA_LABEL.matcher(path);
            if (label.matches()) {
                getCursor().dropParentUntil(Yaml.Document.class::isInstance)
                        .computeMessageIfAbsent("labels", l -> new HashMap<>())
                        .put(label.group(1), value);
            } else {
                Matcher annotation = METADATA_ANNOTATION.matcher(path);
                if (annotation.matches()) {
                    getCursor().dropParentUntil(Yaml.Document.class::isInstance)
                            .computeMessageIfAbsent("annotations", a -> new HashMap<>())
                            .put(annotation.group(1), value);
                }
            }
        }

        return super.visitMappingEntry(entry, p);
    }

    private String getPath() {
        return "/" + getCursor().getPathAsStream()
                .filter(p -> p instanceof Yaml.Mapping.Entry)
                .map(Yaml.Mapping.Entry.class::cast)
                .map(e -> e.getKey().getValue())
                .reduce("", (a, b) -> b + (a.isEmpty() ? "" : "/" + a));
    }
}
