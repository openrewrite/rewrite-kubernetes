package org.openrewrite.kubernetes

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KubernetesParserTest {

    @Test
    fun k8sResourceModel() {
        val manifest = """
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
        """

        val k = KubernetesParser.builder().build().parse(manifest)[0].documents[0]

        assertThat(k.resource.apiVersion).isEqualTo("storage.cnrm.cloud.google.com/v1beta1")
        assertThat(k.resource.kind).isEqualTo("StorageBucket")
        assertThat(k.resource.metadata.name).isEqualTo("sample")
        assertThat(k.resource.metadata.annotations)
            .containsExactlyEntriesOf(mapOf("cnrm.cloud.google.com/force-destroy" to "false"))
        assertThat(k.resource.metadata.labels)
            .containsExactlyEntriesOf(mapOf("label-one" to "value-one"))
    }
}
