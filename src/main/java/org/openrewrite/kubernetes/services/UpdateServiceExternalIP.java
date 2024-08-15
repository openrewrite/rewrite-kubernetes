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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.kubernetes.tree.K8S;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateServiceExternalIP extends Recipe {

    @Option(displayName = "IP to find",
            description = "An `externalIP` address to find in the service's external IPs.",
            example = "192.168.0.1")
    String ipToFind;

    @Option(displayName = "IP to update",
            description = "An `externalIP` address to update to in the service's external IPs.",
            example = "10.10.0.1")
    String ipToUpdate;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/pod-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Update `Service` `externalIP`";
    }

    @Override
    public String getDescription() {
        return "Swap out an IP address with another one in `Service` `externalIP` settings.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        YamlIsoVisitor<ExecutionContext> visitor = new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, ExecutionContext ctx) {
                Cursor c = getCursor();
                if (K8S.Service.inExternalIPs(c)) {
                    Yaml.Scalar s = (Yaml.Scalar) entry.getBlock();
                    if (ipToFind.equals(s.getValue())) {
                        return entry.withBlock(s.withValue(ipToUpdate));
                    }
                }
                return super.visitSequenceEntry(entry, ctx);
            }
        };
        return fileMatcher != null ? Preconditions.check(new FindSourceFiles(fileMatcher), visitor) : visitor;
    }
}
