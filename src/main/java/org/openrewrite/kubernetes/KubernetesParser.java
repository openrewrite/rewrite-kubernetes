/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.kubernetes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class KubernetesParser extends YamlParser {

    private static final Pattern METADATA_LABEL = Pattern.compile("/metadata/labels/(.+)");
    private static final Pattern METADATA_ANNOTATION = Pattern.compile("/metadata/annotations/(.+)");

    public static Builder builder() {
        return new Builder();
    }

    private KubernetesParser() {
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return super.parseInputs(sources, relativeTo, ctx)
                .map(yaml -> yaml instanceof Yaml.Documents ? updateModel((Yaml.Documents) yaml, ctx) : yaml);
    }

    private Yaml.Documents updateModel(Yaml.Documents yaml, ExecutionContext ctx) {
        return (Yaml.Documents) new UpdateKubernetesModel<>().visitNonNull(yaml, ctx);
    }

    public static class Builder extends YamlParser.Builder {
        public KubernetesParser build() {
            return new KubernetesParser();
        }

        @Override
        public String getDslName() {
            return "kubernetes";
        }
    }

}
