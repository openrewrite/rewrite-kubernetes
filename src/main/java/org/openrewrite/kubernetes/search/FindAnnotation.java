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
import org.openrewrite.yaml.search.YamlSearchResult;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindAnnotation extends Recipe {

    @Option(displayName = "Annotation name",
            description = "The name of the annotation to search for the existence of.",
            example = "mycompany.io/annotation")
    String annotationName;

    @Option(displayName = "Value",
            description = "An optional glob expression that will find values that match.",
            example = "value.*",
            required = false)
    @Nullable
    String value;

    @Override
    public String getDisplayName() {
        return "Find annotation";
    }

    @Override
    public String getDescription() {
        return "Find annotations that optionally match a given regex.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        YamlSearchResult found = new YamlSearchResult(this, "found:" + annotationName);
        YamlSearchResult valid = new YamlSearchResult(this, "found:" + value);

        return new ValidatingMappingEntryVisitor("//metadata/annotations", annotationName, value) {
            @Override
            public Yaml.Mapping.Entry visitFoundEntry(Yaml.Mapping.Entry entry, Cursor parent, ExecutionContext ctx) {
                parent.putMessageOnFirstEnclosing(Yaml.Mapping.Entry.class, MESSAGE_KEY, found);
                return entry;
            }

            @Override
            public Yaml.Mapping.Entry visitValidEntry(Yaml.Mapping.Entry entry, Cursor parent, ExecutionContext ctx) {
                parent.putMessageOnFirstEnclosing(Yaml.Mapping.Entry.class, MESSAGE_KEY, valid);
                return super.visitInvalidEntry(entry, parent, ctx);
            }
        };
    }

}