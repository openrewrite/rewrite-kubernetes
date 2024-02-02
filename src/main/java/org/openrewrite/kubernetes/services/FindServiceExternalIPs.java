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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.kubernetes.search.EntryMarkingVisitor;
import org.openrewrite.kubernetes.tree.K8S;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindServiceExternalIPs extends Recipe {

    private static final String FIND_IPS = "findips";

    @Option(displayName = "IP addresses",
            description = "The list of IP addresses of which at least one external IP should .",
            example = "192.168.0.1")
    Set<String> externalIPs;
    @Option(displayName = "Find missing",
            description = "Whether to treat this search as finding Services whose externalIPs do not contain any of " +
                    "the query IPs.",
            required = false)
    boolean findMissing;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/pod-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Find uses of `externalIP`";
    }

    @Override
    public String getDescription() {
        return "Find any `Service` whose `externalIP` list contains, or does not contain, one of a list of IPs.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String result = (findMissing ? "missing" : "found") + " ip";

        EntryMarkingVisitor visitor = new EntryMarkingVisitor() {
            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext ctx) {
                Cursor c = getCursor();
                if (K8S.Service.inExternalIPs(c)) {
                    List<String> ips = sequence.getEntries().stream()
                            .map(e -> ((Yaml.Scalar) e.getBlock()).getValue())
                            .collect(Collectors.toList());

                    boolean matches = findMissing ? ips.stream().noneMatch(externalIPs::contains) :
                            ips.stream().anyMatch(externalIPs::contains);
                    if (matches) {
                        c.putMessageOnFirstEnclosing(Yaml.Mapping.Entry.class, MARKER_KEY, result);
                    }
                }
                return super.visitSequence(sequence, ctx);
            }
        };
        return fileMatcher != null ? Preconditions.check(new FindSourceFiles(fileMatcher), visitor) : visitor;
    }
}
