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

public class KubernetesParser implements Parser<Kubernetes> {
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
