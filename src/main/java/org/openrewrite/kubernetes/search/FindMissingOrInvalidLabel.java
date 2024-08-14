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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.kubernetes.tree.K8S;
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.tree.Yaml;

import java.util.regex.Pattern;

import static org.openrewrite.kubernetes.tree.K8S.asLabels;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindMissingOrInvalidLabel extends Recipe {

    @Option(displayName = "Label name",
            description = "The name of the label to search for the existence of.",
            example = "mylabel")
    String labelName;

    @Option(displayName = "Value",
            description = "An optional regex that will validate values that match.",
            example = "value(.*)",
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
        return "Find label";
    }

    @Override
    public String getDescription() {
        return "Find labels that optionally match a given regex.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern pattern = value != null ? Pattern.compile(value) : null;
        String missing = "missing:" + labelName;
        String invalid = null != value ? ("invalid:" + value) : null;

        EntryMarkingVisitor visitor = new EntryMarkingVisitor() {
            private final JsonPathMatcher IN_LABELS = new JsonPathMatcher("$.*..metadata.labels.*");

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Cursor parent = getCursor().dropParentUntil(is -> is instanceof Yaml.Mapping || is instanceof Yaml.Document);
                if (parent.getValue() instanceof Yaml.Mapping && IN_LABELS.matches(parent)) {
                    K8S.Labels labels = asLabels(parent.getValue());
                    if (value == null && !labels.getKeys().contains(labelName)) {
                        getCursor().getParentOrThrow().putMessageOnFirstEnclosing(Yaml.Mapping.Entry.class, MARKER_KEY, missing);
                    } else if (pattern != null && !labels.valueMatches(labelName, pattern, getCursor())) {
                        getCursor().putMessageOnFirstEnclosing(Yaml.Mapping.Entry.class, MARKER_KEY, invalid);
                    }
                }
                return super.visitMappingEntry(entry, ctx);
            }
        };
        return fileMatcher != null ? Preconditions.check(new FindSourceFiles(fileMatcher), visitor) : visitor;
    }

}
