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

package org.openrewrite.kubernetes.services;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.yaml.XPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.search.YamlSearchResult;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindServiceExternalIPs extends Recipe {

    private static final String FIND_IPS = "findips";

    @Option(displayName = "IP addresses",
            description = "The list of IP addresses of which at least one external IP should .",
            example = "192.168.0.1")
    Set<String> externalIPs;
    @Option(displayName = "Invert query",
            description = "Whether to treat this search as finding Services whose externalIPs do not contain any of " +
                    "the query IPs.")
    boolean invertQuery;

    @Override
    public String getDisplayName() {
        return "Find externalIPs";
    }

    @Override
    public String getDescription() {
        return "Find Services whose externalIPs list contains, or does not contain, one of a list of IPs.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher matcher = new XPathMatcher("/spec/externalIPs");
        YamlSearchResult result = new YamlSearchResult(this, (invertQuery ? "missing" : "found") + " ip");
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext executionContext) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, executionContext);
                if (Boolean.TRUE.equals(getCursor().getMessage(FIND_IPS))) {
                    return e.withMarkers(e.getMarkers().addIfAbsent(result));
                }
                return e;
            }

            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext ctx) {
                Cursor parent = getCursor().getParentOrThrow();
                if (parent.getValue() instanceof Yaml.Mapping.Entry && matcher.matches(parent)) {
                    List<String> ips = sequence.getEntries().stream()
                            .map(e -> ((Yaml.Scalar) e.getBlock()).getValue())
                            .collect(Collectors.toList());
                    boolean queryMatchesThisIp = ips.stream().anyMatch(externalIPs::contains);
                    boolean ipIsMissing = ips.stream().anyMatch(ip -> !externalIPs.contains(ip));
                    if ((!invertQuery && queryMatchesThisIp) || (invertQuery && ipIsMissing)) {
                        getCursor().putMessageOnFirstEnclosing(Yaml.Mapping.Entry.class, FIND_IPS, Boolean.TRUE);
                    }
                }
                return super.visitSequence(sequence, ctx);
            }
        };
    }
}
