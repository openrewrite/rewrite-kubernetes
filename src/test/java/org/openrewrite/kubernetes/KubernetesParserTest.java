package org.openrewrite.kubernetes;

import org.junit.jupiter.api.Test;
import org.openrewrite.kubernetes.tree.KubernetesModel;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class KubernetesParserTest extends KubernetesRecipeTest {

    @Test
    void kubernetesModel() {
        String manifest = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            metadata:
              annotations:
                cnrm.cloud.google.com/force-destroy: "false"
              labels:
                label-one: "value-one"
              name: sample
            spec:
              bucketPolicyOnly: true
              lifecycleRule:
                - action:
                    type: Delete
                  condition:
                    age: 7
              versioning:
                enabled: true
              cors:
                - origin: ["http://example.appspot.com"]
                  responseHeader: ["Content-Type"]
                  method: ["GET", "HEAD", "DELETE"]
                  maxAgeSeconds: 3600
        """;

        KubernetesModel model = getModel(
          KubernetesParser.builder().build().parse(manifest).get(0).getDocuments().get(0));
        assertThat(model.getApiVersion()).isEqualTo("storage.cnrm.cloud.google.com/v1beta1");
        assertThat(model.getKind()).isEqualTo("StorageBucket");
        assertThat(model.getMetadata().getName()).isEqualTo("sample");
        assertThat(model.getMetadata().getAnnotations())
          .containsExactlyEntriesOf(Map.of("cnrm.cloud.google.com/force-destroy", "false"));
        assertThat(model.getMetadata().getLabels())
          .containsExactlyEntriesOf(Map.of("label-one", "value-one"));
    }
}
