package org.openrewrite.kubernetes;

import org.openrewrite.kubernetes.tree.KubernetesModel;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.yaml.tree.Yaml;

class KubernetesRecipeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(KubernetesParser.builder());
    }

    KubernetesModel getModel(Yaml.Document doc) {
        return doc.getMarkers().findFirst(KubernetesModel.class).orElseThrow(() ->
          new IllegalStateException("KubernetesVisitor should not be visiting a YAML document without a KubernetesModel")
        );
    }
}
