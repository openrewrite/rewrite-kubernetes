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

import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.List;

import static java.util.stream.Collectors.toList;

public final class KubernetesParser implements Parser<Kubernetes> {
    YamlParser yamlParser = new YamlParser();

    private KubernetesParser() {
    }

    @Override
    public List<Kubernetes> parse(@Language("yaml") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public List<Kubernetes> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return yamlParser.parseInputs(sources, relativeTo, ctx).stream()
                .map(yaml -> toKubernetes(yaml, ctx))
                .collect(toList());
    }

    @Override
    public boolean accept(Path path) {
        return yamlParser.accept(path);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        public KubernetesParser build() {
            return new KubernetesParser();
        }
    }

    private Kubernetes toKubernetes(Yaml.Documents yaml, ExecutionContext ctx) {
        Yaml.Documents y = (Yaml.Documents) new RefreshModel<>().visit(yaml, ctx);
        assert y != null;
        return new Kubernetes(y);
    }
}
