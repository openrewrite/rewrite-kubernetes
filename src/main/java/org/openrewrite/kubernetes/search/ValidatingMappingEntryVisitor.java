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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.XPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.search.YamlSearchResult;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@EqualsAndHashCode(callSuper = true)
class ValidatingMappingEntryVisitor extends YamlIsoVisitor<ExecutionContext> {

    static final String MESSAGE_KEY = ValidatingMappingEntryVisitor.class.getSimpleName();

    XPathMatcher mappingMatcher;
    String entryKey;
    @Nullable
    Pattern validationPattern;

    public ValidatingMappingEntryVisitor(String mappingPath, String entryKey, @Nullable String validationRegex) {
        this.mappingMatcher = new XPathMatcher(mappingPath);
        this.entryKey = entryKey;
        this.validationPattern = validationRegex == null ? null : Pattern.compile(validationRegex);
    }

    public Yaml.Mapping visitMissingEntry(Yaml.Mapping mapping, Cursor cursor, ExecutionContext ctx) {
        return mapping;
    }

    public Yaml.Mapping.Entry visitInvalidEntry(Yaml.Mapping.Entry entry, Cursor parent, ExecutionContext ctx) {
        return entry;
    }

    public Yaml.Mapping.Entry visitFoundEntry(Yaml.Mapping.Entry entry, Cursor parent, ExecutionContext ctx) {
        return entry;
    }

    public Yaml.Mapping.Entry visitValidEntry(Yaml.Mapping.Entry entry, Cursor parent, ExecutionContext ctx) {
        return entry;
    }

    @Override
    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
        Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
        YamlSearchResult r = getCursor().getMessage(MESSAGE_KEY);
        if (r != null) {
            return e.withMarkers(e.getMarkers().addIfAbsent(r));
        }
        return e;
    }

    @Override
    public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
        Yaml.Mapping m = super.visitMapping(mapping, ctx);
        Cursor parent = getCursor().getParentOrThrow();
        if (!mappingMatcher.matches(parent)) {
            return m;
        }

        AtomicBoolean entryFound = new AtomicBoolean(false);
        List<Yaml.Mapping.Entry> newEntries = ListUtils.map(m.getEntries(), e -> {
            if (!e.getKey().getValue().equals(entryKey)) {
                return e;
            }
            entryFound.compareAndSet(false, true);
            String annoValue = ((Yaml.Scalar) e.getValue()).getValue();
            if (validationPattern == null) {
                return visitFoundEntry(e, getCursor(), ctx);
            } else if (validationPattern.matcher(annoValue).matches()) {
                return visitValidEntry(e, getCursor(), ctx);
            } else {
                return visitInvalidEntry(e, getCursor(), ctx);
            }
        });

        if (!entryFound.get()) {
            m = visitMissingEntry(m, getCursor(), ctx);
        }

        return m.withEntries(newEntries);
    }

}
