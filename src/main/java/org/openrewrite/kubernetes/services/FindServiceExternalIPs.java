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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.XPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.search.YamlSearchResult;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindServiceExternalIPs extends Recipe {

    @Option(displayName = "IP addresses",
            description = "A set of IP addresses to find Services.",
            example = "192.168.0.1")
    Set<String> ipAddresses;

    @Override
    public String getDisplayName() {
        return "IP addresses";
    }

    @Override
    public String getDescription() {
        return "A set of IP addresses to find Services.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher matcher = new XPathMatcher("/spec/externalIPs");
        YamlSearchResult result = new YamlSearchResult(this, "IP");
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext ctx) {
                Cursor parent = getCursor().getParentOrThrow();
                if (parent.getValue() instanceof Yaml.Mapping.Entry && matcher.matches(parent)) {
                    return sequence.withEntries(ListUtils.map(sequence.getEntries(), entry -> {
                        if (entry.getBlock() instanceof Yaml.Scalar) {
                            Yaml.Scalar ip = (Yaml.Scalar) entry.getBlock();
                            if (ipAddresses.contains(ip.getValue())) {
                                return entry.withBlock(ip.withMarkers(ip.getMarkers().addIfAbsent(result)));
                            }
                        }
                        return entry;
                    }));
                }
                return super.visitSequence(sequence, ctx);
            }
        };
    }
}