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
package org.openrewrite.kubernetes

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KubernetesParserTest : KubernetesRecipeTest {

    @Test
    fun kubernetesModel() {
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

        val model = getModel(KubernetesParser.builder().build().parse(manifest)[0].documents[0])


        assertThat(model.apiVersion).isEqualTo("storage.cnrm.cloud.google.com/v1beta1")
        assertThat(model.kind).isEqualTo("StorageBucket")
        assertThat(model.metadata.name).isEqualTo("sample")
        assertThat(model.metadata.annotations)
            .containsExactlyEntriesOf(mapOf("cnrm.cloud.google.com/force-destroy" to "false"))
        assertThat(model.metadata.labels)
            .containsExactlyEntriesOf(mapOf("label-one" to "value-one"))
    }
}
