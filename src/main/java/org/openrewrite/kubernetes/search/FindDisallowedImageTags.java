package org.openrewrite.kubernetes.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.kubernetes.ContainerImage;
import org.openrewrite.yaml.XPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.search.YamlSearchResult;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindDisallowedImageTags extends Recipe {

    @Option(displayName = "Disallowed tags",
            description = "The set of image tags to find which are considered disallowed.",
            example = "latest")
    Set<String> disallowedTags;

    @Override
    public String getDisplayName() {
        return "Disallowed tags";
    }

    @Override
    public String getDescription() {
        return "The set of image tags to find which are considered disallowed.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher imageMatcher = new XPathMatcher("//spec/containers/image");
        XPathMatcher initImageMatcher = new XPathMatcher("//spec/initContainers/image");

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, ExecutionContext executionContext) {
                Cursor parent = getCursor().getParentOrThrow();
                if (imageMatcher.matches(parent) || initImageMatcher.matches(parent)) {
                    ContainerImage image = new ContainerImage(scalar);
                    if (disallowedTags.stream().anyMatch(t -> t.equals(image.getImageName().getTag()))) {
                        return scalar.withMarkers(scalar.getMarkers().addIfAbsent(new YamlSearchResult(
                                FindDisallowedImageTags.this,
                                "disallowed tag: " + disallowedTags
                        )));
                    }
                }
                return super.visitScalar(scalar, executionContext);
            }
        };
    }

}
