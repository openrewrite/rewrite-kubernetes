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

package org.openrewrite.kubernetes.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kubernetes.tree.K8S;
import org.openrewrite.yaml.tree.Yaml;

import java.util.regex.Pattern;

import static org.openrewrite.kubernetes.tree.K8S.Annotations.inAnnotations;
import static org.openrewrite.kubernetes.tree.K8S.asAnnotations;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindMissingOrInvalidAnnotation extends Recipe {

    @Option(displayName = "Annotation name",
            description = "The name of the annotation to search for the existence of.",
            example = "mycompany.io/annotation")
    String annotationName;

    @Option(displayName = "Value",
            description = "An optional regex that will validate values that match.",
            example = "value.*",
            required = false)
    @Nullable
    String value;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/pod-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Find annotation";
    }

    @Override
    public String getDescription() {
        return "Find annotations that optionally match a given value.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern pattern = value != null ? Pattern.compile(value) : null;
        String missing = "missing:" + annotationName;
        String invalid = null != value ? ("invalid:" + value) : null;

        EntryMarkingVisitor visitor = new EntryMarkingVisitor() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Cursor c = getCursor();
                if (inAnnotations(c)) {
                    K8S.Annotations annos = asAnnotations(c.firstEnclosing(Yaml.Mapping.class));
                    if (value == null && !annos.getKeys().contains(annotationName)) {
                        c.getParentOrThrow().putMessageOnFirstEnclosing(Yaml.Mapping.Entry.class, MARKER_KEY, missing);
                    } else if (pattern != null && !annos.valueMatches(annotationName, pattern, c)) {
                        c.putMessageOnFirstEnclosing(Yaml.Mapping.Entry.class, MARKER_KEY, invalid);
                    }
                }
                return super.visitMappingEntry(entry, ctx);
            }
        };
        return fileMatcher != null ? Preconditions.check(new HasSourcePath(fileMatcher), visitor) : visitor;
    }
}
