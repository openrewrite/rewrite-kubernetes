/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.kubernetes.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kubernetes.resource.ResourceLimit;
import org.openrewrite.marker.Marker;
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.emptySet;
import static org.openrewrite.Tree.randomId;

public interface K8S extends Marker {


    static boolean inKind(String kind, Cursor cursor) {
        Yaml.Document doc = cursor.firstEnclosing(Yaml.Document.class);
        if (doc == null) {
            return false;
        }
        Resource r = K8S.asResource((Yaml.Mapping) doc.getBlock());
        return kind.equals(r.getKind());
    }

    static boolean inPod(Cursor cursor) {
        return inKind("Pod", cursor);
    }

    static boolean inDaemonSet(Cursor cursor) {
        return inKind("DaemonSet", cursor);
    }

    static boolean inStatefulSet(Cursor cursor) {
        return inKind("StatefulSet", cursor);
    }

    static boolean inDeployment(Cursor cursor) {
        return inKind("Deployment", cursor);
    }

    static boolean inService(Cursor cursor) {
        return inKind("Service", cursor);
    }

    static Resource asResource(Yaml.Mapping m) {
        String apiVersion = null;
        String kind = null;
        for (Yaml.Mapping.Entry e : m.getEntries()) {
            Yaml.Block value = e.getValue();
            if ("apiVersion".equals(e.getKey().getValue())) {
                apiVersion = ((Yaml.Scalar) value).getValue();
            } else if ("kind".equals(e.getKey().getValue())) {
                kind = ((Yaml.Scalar) value).getValue();
            }
        }
        return new Resource(randomId(), apiVersion, kind);
    }

    @SuppressWarnings("ConstantConditions")
    static Metadata asMetadata(Yaml.Mapping m) {
        String namespace = null;
        String name = "";
        Annotations annotations = null;
        Labels labels = null;
        for (Yaml.Mapping.Entry e : m.getEntries()) {
            String key = e.getKey().getValue();
            Object value;
            if (e.getValue() instanceof Yaml.Scalar) {
                value = ((Yaml.Scalar) e.getValue()).getValue();
            } else {
                value = e.getValue();
            }
            switch (key) {
                case "namespace":
                    namespace = (String) value;
                    break;
                case "name":
                    name = (String) value;
                    break;
                case "labels":
                    labels = asLabels((Yaml.Mapping) value);
                    break;
                case "annotations":
                    annotations = asAnnotations((Yaml.Mapping) value);
                    break;
            }
        }
        return new Metadata(randomId(), namespace, name, annotations, labels);
    }

    static Annotations asAnnotations(@Nullable Yaml.Mapping m) {
        if (m == null) {
            return new Annotations(randomId(), emptySet());
        }
        Set<String> keys = new HashSet<>();
        for (Yaml.Mapping.Entry e : m.getEntries()) {
            keys.add(e.getKey().getValue());
        }
        return new Annotations(randomId(), keys);
    }

    static Labels asLabels(@Nullable Yaml.Mapping m) {
        if (m == null) {
            return new Labels(randomId(), emptySet());
        }
        Set<String> keys = new HashSet<>();
        for (Yaml.Mapping.Entry e : m.getEntries()) {
            keys.add(e.getKey().getValue());
        }
        return new Labels(randomId(), keys);
    }

    static ResourceLimits asResourceLimits(@Nullable Yaml.Scalar s) {
        if (null == s) {
            return new ResourceLimits(randomId(), null);
        } else {
            return new ResourceLimits(randomId(), new ResourceLimit(s.getValue()));
        }
    }

    static @Nullable Service asService(@Nullable Yaml.Mapping m) {
        if (m == null) {
            return null;
        }
        return new Service(randomId(), m.getEntries().stream()
                .filter(e -> "type".equals(e.getKey().getValue()))
                .findFirst()
                .map(e -> ((Yaml.Scalar) e.getValue()).getValue())
                .orElse("ClusterIP"));
    }

    static boolean inMappingEntry(String jsonPath, @Nullable Cursor cursor) {
        return inMappingEntry(new JsonPathMatcher(jsonPath), cursor);
    }

    static boolean inMappingEntry(JsonPathMatcher jsonPath, @Nullable Cursor cursor) {
        return firstEnclosingEntryMatching(jsonPath, cursor).isPresent();
    }

    static Optional<Cursor> firstEnclosingEntryMatching(String jsonPath, @Nullable Cursor cursor) {
        return firstEnclosingEntryMatching(new JsonPathMatcher(jsonPath), cursor);
    }

    static Optional<Cursor> firstEnclosingEntryMatching(JsonPathMatcher jsonPath, @Nullable Cursor cursor) {
        if (cursor == null) {
            return Optional.empty();
        }

        Yaml.Mapping.Entry e = cursor.getValue() instanceof Yaml.Mapping.Entry ? cursor.getValue() : cursor.firstEnclosing(Yaml.Mapping.Entry.class);
        if (e == null || e.getKey() == cursor.getValue()) {
            return Optional.empty();
        }

        return jsonPath.find(cursor)
                .flatMap(found -> {
                    if (found instanceof Yaml.Mapping) {
                        return ((Yaml.Mapping) found).getEntries().stream()
                                .map(o -> {
                                    if (o.getValue() == cursor.getValue()) {
                                        return cursor;
                                    }
                                    return null;
                                }).filter(Objects::nonNull)
                                .findFirst();
                    } else if (found instanceof List) {
                        //noinspection unchecked
                        return ((List<Object>) found).stream()
                                .map(o -> {
                                    Cursor c = cursor;
                                    while (c != null && c.getValue() != o) {
                                        c = c.getParent();
                                    }
                                    return c;
                                })
                                .filter(Objects::nonNull)
                                .findFirst();
                    }

                    if (found == cursor.getValue() || (found instanceof Yaml.Mapping.Entry && ((Yaml.Mapping.Entry) found).getValue() == cursor.getValue())) {
                        return Optional.of(cursor);
                    }

                    return Optional.empty();
                });
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = false)
    @Data
    class Resource implements K8S {
        @EqualsAndHashCode.Include
        @With
        UUID id;
        String apiVersion;
        String kind;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = false)
    @Data
    class Metadata implements K8S {
        @EqualsAndHashCode.Include
        @With
        UUID id;
        @Nullable
        String namespace;
        String name;
        @Nullable
        Annotations annotations;
        @Nullable
        Labels labels;

        public static boolean isMetadata(Cursor cursor) {
            return firstEnclosingEntryMatching("$.metadata", cursor)
                    .filter(c -> c.getValue() == cursor.getValue())
                    .isPresent();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = false)
    @Data
    class Annotations implements K8S {
        @EqualsAndHashCode.Include
        @With
        UUID id;
        Set<String> keys;

        public static boolean inAnnotations(Cursor cursor) {
            Cursor parent = cursor.dropParentUntil(is -> is instanceof Yaml.Mapping || is instanceof Yaml.Document);
            if (parent.getValue() instanceof Yaml.Mapping) {
                return new JsonPathMatcher("$.*..metadata.annotations.*").matches(parent);
            }
            return false;
        }

        public boolean valueMatches(String name, Pattern regex, Cursor cursor) {
            Yaml.Mapping.Entry e = (cursor.getValue() instanceof Yaml.Mapping.Entry) ? cursor.getValue() : cursor.firstEnclosing(Yaml.Mapping.Entry.class);
            if (e == null) {
                return false;
            }
            if (!name.equals(e.getKey().getValue())) {
                return false;
            }
            if (!(e.getValue() instanceof Yaml.Scalar)) {
                return false;
            }
            String value = ((Yaml.Scalar) e.getValue()).getValue();
            return regex.matcher(value).matches();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = false)
    @Data
    class Labels implements K8S {
        @EqualsAndHashCode.Include
        @With
        UUID id;
        Set<String> keys;

        public static boolean inLabels(Cursor cursor) {
            return inMappingEntry("..metadata.labels.*", cursor);
        }

        public boolean valueMatches(String name, Pattern regex, Cursor cursor) {
            Yaml.Mapping.Entry e = (cursor.getValue() instanceof Yaml.Mapping.Entry) ? cursor.getValue() : cursor.firstEnclosing(Yaml.Mapping.Entry.class);
            if (e == null) {
                return false;
            }
            if (!name.equals(e.getKey().getValue())) {
                return false;
            }
            if (!(e.getValue() instanceof Yaml.Scalar)) {
                return false;
            }
            String value = ((Yaml.Scalar) e.getValue()).getValue();
            return regex.matcher(value).matches();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = false)
    @Data
    class Pod implements K8S {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        public static boolean inSpec(Cursor cursor) {
            return inMappingEntry("..spec.*", cursor);
        }

    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = false)
    @Data
    class Containers implements K8S {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        public static boolean inContainerSpec(Cursor cursor) {
            return inMappingEntry("$.*..spec.containers[*].*", cursor);
        }

        public static boolean isImageName(Cursor cursor) {
            return cursor.getPathAsStream(o -> (o instanceof Yaml.Mapping.Entry && "image".equals(((Yaml.Mapping.Entry) o).getKey().getValue()))).findFirst().isPresent();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = false)
    @Data
    class InitContainers implements K8S {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        public static boolean inInitContainerSpec(Cursor cursor) {
            return inMappingEntry("$.*..spec.initContainers[*].*", cursor);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = false)
    @Data
    class ResourceLimits implements K8S {
        @EqualsAndHashCode.Include
        @With
        UUID id;
        ResourceLimit value;

        public static boolean inResources(Cursor cursor) {
            return inMappingEntry("$.*..spec.containers[*].resources", cursor);
        }

        public static boolean inLimits(String type, Cursor cursor) {
            return inMappingEntry("$.*..spec.containers[*].resources.limits." + type, cursor);
        }

        public static boolean inRequests(String type, Cursor cursor) {
            return inMappingEntry("$.*..spec.containers[*].resources.requests." + type, cursor);
        }

    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = false)
    @Data
    class Service implements K8S {
        @EqualsAndHashCode.Include
        @With
        UUID id;
        @Nullable
        String type;

        public static boolean isServiceSpec(Cursor cursor) {
            return firstEnclosingEntryMatching("$.spec", cursor)
                    .filter(c -> c == cursor)
                    .isPresent();
        }

        public static boolean inServiceSpec(Cursor cursor) {
            return inMappingEntry("$.spec", cursor);
        }

        public static boolean inExternalIPs(Cursor cursor) {
            return cursor.getPathAsStream(o -> o instanceof Yaml.Mapping.Entry && "externalIPs".equals(((Yaml.Mapping.Entry) o).getKey().getValue())).findFirst().isPresent();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = false)
    @Data
    class Ingress implements K8S {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        public static boolean isTlsConfigured(Cursor cursor) {
            Optional<Object> tls = new JsonPathMatcher("$.spec.tls[*].hosts").find(cursor);
            return tls.isPresent();
        }

        public static boolean isDisallowHttpConfigured(Cursor cursor) {
            Optional<Object> tls =
                    new JsonPathMatcher("$.metadata.annotations['kubernetes.io/ingress.allow-http']").find(cursor);
            return tls.map(o -> {
                if (o instanceof Yaml.Mapping.Entry) {
                    Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) o;
                    if (e.getValue() instanceof Yaml.Scalar) {
                        return "false".equals(((Yaml.Scalar) e.getValue()).getValue());
                    }
                }
                return false;
            }).orElse(false);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = false)
    @Data
    class RBAC implements K8S {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        public static boolean inRules(Cursor cursor) {
            return inMappingEntry("$.rules", cursor);
        }
    }

}
