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

import org.openrewrite.kubernetes.tree.KubernetesModel;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.Tree.randomId;

public class UpdateKubernetesModel<P> extends YamlIsoVisitor<P> {
    private static final Pattern METADATA_LABEL = Pattern.compile("/metadata/labels/(.+)");
    private static final Pattern METADATA_ANNOTATION = Pattern.compile("/metadata/annotations/(.+)");

    @Override
    public Yaml.Document visitDocument(Yaml.Document document, P p) {
        Yaml.Document d = super.visitDocument(document, p);

        KubernetesModel resource = new KubernetesModel(
                randomId(),
                getCursor().getMessage("apiVersion"),
                getCursor().getMessage("kind"),
                new KubernetesModel.Metadata(
                        getCursor().getMessage("namespace"),
                        getCursor().getMessage("name"),
                        getCursor().getMessage("annotations"),
                        getCursor().getMessage("labels")
                )
        );
        if (resource.getApiVersion() != null && resource.getKind() != null) {
            return d.withMarkers(document.getMarkers().addIfAbsent(resource));
        } else {
            return d;
        }
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
                case "/metadata/namespace":
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, "namespace", value);
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
                .filter(org.openrewrite.yaml.tree.Yaml.Mapping.Entry.class::isInstance)
                .map(Yaml.Mapping.Entry.class::cast)
                .map(e -> e.getKey().getValue())
                .reduce("", (a, b) -> b + (a.isEmpty() ? "" : "/" + a));
    }
}
