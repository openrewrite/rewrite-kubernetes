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
package org.openrewrite.kubernetes.resource

import org.junit.jupiter.api.Test
import org.openrewrite.kubernetes.KubernetesRecipeTest
import org.openrewrite.kubernetes.resource.FindExceedsResourceValue

class FindExceedsResourceRatioTest : KubernetesRecipeTest {

    @Test
    fun `must find limits that exceed a given maximum ratio`() = assertChanged(
        recipe = FindExceedsResourceRatio(
            "memory",
            "2"
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - image: nginx:latest
                resources:
                    limits:
                        cpu: "2Gi"
                        memory: "1Gi"
                    requests:
                        cpu: "100Mi"
                        memory: "64m"
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: application
            spec:
              containers:            
              - image: nginx:latest
                ~~(exceeds max memory limits/requests ratio of 2)~~>resources:
                    limits:
                        cpu: "2Gi"
                        memory: "1Gi"
                    requests:
                        cpu: "100Mi"
                        memory: "64m"
        """
    )

}
