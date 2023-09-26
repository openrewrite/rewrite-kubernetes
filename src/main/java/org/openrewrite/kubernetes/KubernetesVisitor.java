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

import org.openrewrite.TreeVisitor;
import org.openrewrite.kubernetes.tree.KubernetesModel;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

public class KubernetesVisitor<P> extends YamlVisitor<P> {

    protected KubernetesModel getKubernetesModel() {
        Yaml.Document doc = getCursor().firstEnclosing(Yaml.Document.class);
        if (doc != null) {
            return getKubernetesModel(doc);
        }
        throw new IllegalStateException("KubernetesVisitor should not be visiting a YAML document without a KubernetesModel");
    }

    public void maybeUpdateModel() {
        for (TreeVisitor<?, P> afterVisit : getAfterVisit()) {
            if (afterVisit instanceof UpdateKubernetesModel) {
                return;
            }
        }
        doAfterVisit(new UpdateKubernetesModel<>());
    }

    public static KubernetesModel getKubernetesModel(Yaml.Document doc) {
        return doc.getMarkers()
                .findFirst(KubernetesModel.class)
                .orElseThrow(() -> new IllegalStateException("KubernetesVisitor should not be visiting a YAML document without a KubernetesModel"));
    }
}
