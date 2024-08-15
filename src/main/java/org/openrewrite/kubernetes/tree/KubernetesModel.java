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
package org.openrewrite.kubernetes.tree;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;

import java.util.Map;
import java.util.UUID;

@Value
public class KubernetesModel implements Marker {

    @With
    UUID id;

    String apiVersion;
    String kind;
    Metadata metadata;

    @Value
    public static class Metadata {
        @Nullable
        String namespace;

        @Nullable
        String name;

        @Nullable
        Map<String, String> annotations;

        @Nullable
        Map<String, String> labels;
    }
}
