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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.yaml.XPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpdateServiceExternalIP extends Recipe {

    @Option(displayName = "IP to find",
            description = "An ExternalIP address to find in the service's external IPs.",
            example = "192.168.0.1")
    String ipToFind;
    @Option(displayName = "IP to update",
            description = "An ExternalIP address to update to in the service's external IPs.",
            example = "10.10.0.1")
    String ipToUpdate;

    @Override
    public String getDisplayName() {
        return "Update Service ExternalIPs";
    }

    @Override
    public String getDescription() {
        return "Swap out an IP address with another one in the Service externalIPs setting.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher matcher = new XPathMatcher("/spec/externalIPs");

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, ExecutionContext ctx) {
                if (matcher.matches(getCursor().getParentOrThrow().getParentOrThrow())) {
                    Yaml.Scalar s = (Yaml.Scalar) entry.getBlock();
                    if (ipToFind.equals(s.getValue())) {
                        return entry.withBlock(s.withValue(ipToUpdate));
                    }
                }
                return super.visitSequenceEntry(entry, ctx);
            }
        };
    }
}