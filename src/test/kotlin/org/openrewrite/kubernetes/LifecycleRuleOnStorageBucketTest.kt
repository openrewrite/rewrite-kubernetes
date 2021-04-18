package org.openrewrite.kubernetes

import org.junit.jupiter.api.Test
import org.openrewrite.config.Environment

class LifecycleRuleOnStorageBucketTest: KubernetesRecipeTest {

    @Test
    fun lifecycleRuleOnStorageBucket() = assertChanged(
        recipe = Environment.builder()
            .scanRuntimeClasspath()
            .build()
            .activateRecipes("org.openrewrite.kubernetes.LifecycleRuleOnStorageBucket"),
        before = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
              bucketPolicyOnly: true
        """,
        after = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
              bucketPolicyOnly: true
              lifecycleRule:
                - action:
                    type: Delete
                  condition:
                    age: 7
        """
    )
}
