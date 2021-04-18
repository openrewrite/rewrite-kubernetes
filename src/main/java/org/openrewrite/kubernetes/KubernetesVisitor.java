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
import org.openrewrite.yaml.YamlVisitor;

@SuppressWarnings("NotNullFieldNotInitialized")
public class KubernetesVisitor<P> extends YamlVisitor<P> {
    protected KubernetesModel model;

    public Kubernetes.ResourceDocument visitKubernetesResource(Kubernetes.ResourceDocument resource, P p) {
        this.model = resource.getModel();
        return (Kubernetes.ResourceDocument) super.visitDocument(resource, p);
    }
}
