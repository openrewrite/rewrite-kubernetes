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

class AddConfigurationTest : KubernetesRecipeTest {

    @Test
    fun `add configuration to mapping entry if the path does not exist`() = assertChanged(
        recipe = AddConfiguration(
            null,
            "Pod",
            "$.spec",
            "privileged: false"
        ),
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
              privileged: false
        """
    )

    @Test
    fun `add configuration to nested mapping entries if the path does not exist`() = assertChanged(
        recipe = AddConfiguration(
            null,
            "PodSecurityPolicy",
            "$",
            """
                spec:
                  privileged: false
            """.trimIndent()
        ),
        before = """
            apiVersion: policy/v1beta1
            kind: PodSecurityPolicy
            metadata:
              name: psp
        """,
        after = """
            apiVersion: policy/v1beta1
            kind: PodSecurityPolicy
            metadata:
              name: psp
            spec:
              privileged: false
        """
    )

    @Test
    fun `add configuration to sequence entries if the path does not exist`() = assertChanged(
        recipe = AddConfiguration(
            null,
            "Pod",
            "$.spec.containers",
            "imagePullPolicy: Always"
        ),
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
    fun `do not add sequences if the path has no existing entries`() = assertUnchanged(
        recipe = AddConfiguration(
            null,
            "Pod",
            "$.spec.containers",
            "imagePullPolicy: Always"
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: example
            spec:
              containers: []
        """
    )

    @Test
    fun `add configuration to mapping entries within sequence entries if the path does not exist`() = assertChanged(
        recipe = AddConfiguration(
            null,
            "Pod",
            "$.spec.containers",
            """
                securityContext:
                  allowPrivilegeEscalation: false
            """.trimIndent()
        ),
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
    fun `do not modify existing configuration`() = assertUnchanged(
        recipe = AddConfiguration(
            null,
            "PodSecurityPolicy",
            "$.spec",
            "privileged: false"
        ),
        before = """
          apiVersion: policy/v1beta1
          kind: PodSecurityPolicy
          metadata:
            name: psp
          spec:
            privileged: true
        """
    )

}
