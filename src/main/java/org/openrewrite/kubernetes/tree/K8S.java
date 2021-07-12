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
import lombok.experimental.FieldDefaults;
import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kubernetes.resource.ResourceLimit;
import org.openrewrite.marker.Marker;
import org.openrewrite.yaml.XPathMatcher;
import org.openrewrite.yaml.tree.Yaml;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
            switch (e.getKey().getValue()) {
                case "apiVersion":
                    apiVersion = ((Yaml.Scalar) value).getValue();
                    break;
                case "kind":
                    kind = ((Yaml.Scalar) value).getValue();
                    break;
            }
        }
        return new Resource(randomId(), apiVersion, kind);
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

    static boolean inMappingEntry(String xpath, @Nullable Cursor cursor) {
        return inMappingEntry(new XPathMatcher(xpath), cursor);
    }

    static boolean inMappingEntry(XPathMatcher xpath, @Nullable Cursor cursor) {
        return firstEnclosingEntryMatching(xpath, cursor).isPresent();
    }

    static Optional<Cursor> firstEnclosingEntryMatching(String xpath, @Nullable Cursor cursor) {
        return firstEnclosingEntryMatching(new XPathMatcher(xpath), cursor);
    }

    static Optional<Cursor> firstEnclosingEntryMatching(XPathMatcher xpath, @Nullable Cursor cursor) {
        if (cursor == null) {
            return Optional.empty();
        }

        Yaml.Mapping.Entry e = cursor.getValue() instanceof Yaml.Mapping.Entry ? cursor.getValue() : cursor.firstEnclosing(Yaml.Mapping.Entry.class);
        if (e == null || e.getKey() == cursor.getValue()) {
            return Optional.empty();
        } else if (xpath.matches(cursor)) {
            return Optional.of(cursor);
        } else {
            return firstEnclosingEntryMatching(xpath, cursor.getParent());
        }

    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Resource implements K8S {
        @EqualsAndHashCode.Include
        UUID id;
        String apiVersion;
        String kind;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Metadata implements K8S {
        @EqualsAndHashCode.Include
        UUID id;
        @Nullable
        String namespace;
        String name;

    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Annotations {
        @EqualsAndHashCode.Include
        UUID id;
        Set<String> keys;

        public static boolean inAnnotations(Cursor cursor) {
            return inMappingEntry("//metadata/annotations/*", cursor);
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
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Pod implements K8S {
        @EqualsAndHashCode.Include
        UUID id;

        public static boolean inSpec(Cursor cursor) {
            return inMappingEntry("//spec/*", cursor);
        }

    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Containers implements K8S {
        @EqualsAndHashCode.Include
        UUID id;

        public static boolean inContainerSpec(Cursor cursor) {
            return inMappingEntry("//spec/containers", cursor);
        }

        public static boolean isImageName(Cursor cursor) {
            return firstEnclosingEntryMatching("//image", cursor)
                    //.filter(c -> c == cursor.getParent())
                    .isPresent();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class InitContainers implements K8S {
        @EqualsAndHashCode.Include
        UUID id;

        public static boolean inInitContainerSpec(Cursor cursor) {
            return inMappingEntry("//spec/initContainers", cursor);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class ResourceLimits implements K8S {
        @EqualsAndHashCode.Include
        UUID id;
        ResourceLimit value;

        public static boolean inLimits(String type, Cursor cursor) {
            return inMappingEntry("//spec/containers/resources/limits/" + type, cursor);
        }

        public static boolean inRequests(String type, Cursor cursor) {
            return inMappingEntry("//spec/containers/resources/requests/" + type, cursor);
        }

    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Service implements K8S {
        @EqualsAndHashCode.Include
        UUID id;
        @Nullable
        String type;

        public static boolean isServiceSpec(Cursor cursor) {
            return firstEnclosingEntryMatching("/spec", cursor)
                    .filter(c -> c == cursor.getParent())
                    .isPresent();
        }

        public static boolean inServiceSpec(Cursor cursor) {
            return inMappingEntry("/spec", cursor);
        }

        public static boolean inExternalIPs(Cursor cursor) {
            return inMappingEntry("/spec/externalIPs", cursor);
        }
    }

}
