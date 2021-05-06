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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kubernetes.tree.KubernetesModel;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;

public class Kubernetes extends Yaml.Documents {
    public Kubernetes(Yaml.Documents documents) {
        //noinspection unchecked
        super(
                documents.getId(),
                documents.getMarkers(),
                documents.getSourcePath(),
                ListUtils.map((List<Document>) documents.getDocuments(), doc -> doc instanceof ResourceDocument ?
                        doc : new ResourceDocument(doc))
        );
    }

    @Override
    public List<Kubernetes.ResourceDocument> getDocuments() {
        //noinspection unchecked
        return (List<ResourceDocument>) super.getDocuments();
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        if (v instanceof YamlVisitor) {
            Yaml.Documents k8s = (Documents) super.accept(v, p);
            if (k8s != this) {
                return k8s == null ? null : (R) new Kubernetes(k8s);
            }
            return (R) this;
        }
        return v.defaultValue(this, p);
    }

    @Override
    public Kubernetes withMarkers(Markers markers) {
        Yaml.Documents k = super.withMarkers(markers);
        if (k instanceof Kubernetes) {
            return (Kubernetes) k;
        }
        return new Kubernetes(k);
    }

    public static class ResourceDocument extends Yaml.Document {
        @Getter
        private final KubernetesModel model;

        public ResourceDocument(Yaml.Document document) {
            super(
                    document.getId(),
                    document.getPrefix(),
                    document.getMarkers(),
                    document.isExplicit(),
                    document.getBlock(),
                    document.getEnd()
            );

            //noinspection ConstantConditions
            model = document.getMarkers()
                    .findFirst(KubernetesModel.class)
                    .orElse(null);

            assert model != null;
        }

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            if (v instanceof KubernetesVisitor) {
                return ((KubernetesVisitor<P>) v).visitKubernetes(this, p);
            }
            return super.acceptYaml(v, p);
        }

        @Override
        public ResourceDocument withMarkers(Markers markers) {
            Yaml.Document k = super.withMarkers(markers);
            if (k instanceof ResourceDocument) {
                return (ResourceDocument) k;
            }
            return new ResourceDocument(k);
        }
    }
}
