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

import org.junit.jupiter.api.Test
import org.openrewrite.config.Environment

class KubernetesBestPracticesTest : KubernetesRecipeTest {
    private val env: Environment = Environment.builder()
        .scanRuntimeClasspath()
        .build()

    @Test
    fun noPrivilegedContainers() = assertChanged(
        recipe = env.activateRecipes("org.openrewrite.kubernetes.NoPrivilegedContainers"),
        before = """
            apiVersion: policy/v1beta1
            kind: PodSecurityPolicy
            metadata:
              name: example
            spec:
              seLinux:
                rule: RunAsAny
        """,
        after = """
            apiVersion: policy/v1beta1
            kind: PodSecurityPolicy
            metadata:
              name: example
            spec:
              seLinux:
                rule: RunAsAny
              privileged: false
        """
    )

    @Test
    fun lifecycleRuleOnStorageBucket() = assertChanged(
        recipe = env.activateRecipes("org.openrewrite.kubernetes.LifecycleRuleOnStorageBucket"),
        before = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            metadata:
              name: example
            spec:
              bucketPolicyOnly: true
        """,
        after = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            metadata:
              name: example
            spec:
              bucketPolicyOnly: true
              lifecycleRule:
                - action:
                    type: Delete
                  condition:
                    age: 7
        """
    )

    @Test
    fun noHostProcessIdSharing() = assertChanged(
        recipe = env.activateRecipes("org.openrewrite.kubernetes.NoHostProcessIdSharing"),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: example
            spec:
              containers:
                - name: pod
                  image: busybox
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: example
            spec:
              containers:
                - name: pod
                  image: busybox
              hostPID: false
        """
    )

    @Test
    fun noHostIPCSharing() = assertChanged(
        recipe = env.activateRecipes("org.openrewrite.kubernetes.NoHostIPCSharing"),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: example
            spec:
              containers:
                - name: pod
                  image: busybox
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: example
            spec:
              containers:
                - name: pod
                  image: busybox
              hostIPC: false
        """
    )

    @Test
    fun noRootContainers() = assertChanged(
        recipe = env.activateRecipes("org.openrewrite.kubernetes.NoRootContainers"),
        before = """
            apiVersion: policy/v1beta1
            kind: PodSecurityPolicy
            metadata:
              name: example
            spec:
              privileged: false
              seLinux:
                rule: RunAsAny
        """,
        after = """
            apiVersion: policy/v1beta1
            kind: PodSecurityPolicy
            metadata:
              name: example
            spec:
              privileged: false
              seLinux:
                rule: RunAsAny
              runAsUser:
                rule: MustRunAsNonRoot
        """
    )

    @Test
    fun imagePullPolicyAlways() = assertChanged(
        recipe = env.activateRecipes("org.openrewrite.kubernetes.ImagePullPolicyAlways"),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: example
            spec:
              containers:
                - name: pod-0
                  image: busybox
                - name: pod-1
                  image: busybox
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: example
            spec:
              containers:
                - name: pod-0
                  image: busybox
                  imagePullPolicy: Always
                - name: pod-1
                  image: busybox
                  imagePullPolicy: Always
        """
    )

    @Test
    fun noPrivilegeEscalation() = assertChanged(
        recipe = env.activateRecipes("org.openrewrite.kubernetes.NoPrivilegeEscalation"),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: nginx
            spec:
              containers:
              - name: nginx
                image: nginx
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: nginx
            spec:
              containers:
              - name: nginx
                image: nginx
                securityContext:
                  allowPrivilegeEscalation: false
        """
    )

    @Test
    fun noHostNetworkSharing() = assertChanged(
        recipe = env.activateRecipes("org.openrewrite.kubernetes.NoHostNetworkSharing"),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: nginx
            spec:
              containers:
              - name: nginx
                image: nginx
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: nginx
            spec:
              containers:
              - name: nginx
                image: nginx
              hostNetwork: false
        """
    )

    @Test
    fun readOnlyRootFilesystem() = assertChanged(
        recipe = env.activateRecipes("org.openrewrite.kubernetes.ReadOnlyRootFilesystem"),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: nginx
            spec:
              containers:
              - name: nginx
                image: nginx
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: nginx
            spec:
              containers:
              - name: nginx
                image: nginx
                securityContext:
                  readOnlyRootFilesystem: false
        """
    )

    @Test
    fun limitContainerCapabilities() = assertChanged(
        recipe = env.activateRecipes("org.openrewrite.kubernetes.LimitContainerCapabilities"),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: nginx
            spec:
              containers:
              - name: nginx
                image: nginx
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: nginx
            spec:
              containers:
              - name: nginx
                image: nginx
                securityContext:
                  capabilities:
                    drop:
                      - ALL
        """
    )

}
