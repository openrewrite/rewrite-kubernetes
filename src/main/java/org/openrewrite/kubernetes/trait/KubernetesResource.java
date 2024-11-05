/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.kubernetes.trait;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.kubernetes.UpdateKubernetesModel;
import org.openrewrite.kubernetes.tree.KubernetesModel;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.yaml.tree.Yaml;

@Value
public class KubernetesResource implements Trait<Yaml.Document> {

    Cursor cursor;
    KubernetesModel model;

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Matcher extends SimpleTraitMatcher<KubernetesResource> {

        @Nullable
        String apiVersion;
        @Nullable
        String kind;

        @Override
        protected @Nullable KubernetesResource test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Yaml.Document) {
                return new UpdateKubernetesModel<ExecutionContext>()
                        .visitNonNull((Yaml.Document) value, new InMemoryExecutionContext(), cursor.getParent())
                        .getMarkers()
                        .findFirst(KubernetesModel.class)
                        .filter(model -> apiVersion == null || apiVersion.equals(model.getApiVersion()))
                        .filter(model -> kind == null || kind.equals(model.getKind()))
                        .map(kubernetesModel -> new KubernetesResource(cursor, kubernetesModel))
                        .orElse(null);
            }
            return null;
        }
    }
}
